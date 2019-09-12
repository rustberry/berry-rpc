package berryrpc.registry.zkregistry;

import berryrpc.registry.ServiceRegistry;
import lombok.extern.slf4j.Slf4j;
import org.apache.zookeeper.AsyncCallback.StringCallback;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.KeeperException.Code;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.ZooKeeper;

import java.io.IOException;

@Slf4j
public class ZKRegistry extends AbstractZooKeeperClient implements ServiceRegistry {
    private String registryHost;
    private ZooKeeper zk;

    public ZKRegistry(ZooKeeper zooKeeper) {
        this.zk = zooKeeper;
        bootstrap();
    }

    public ZKRegistry(String registryHost) throws IOException {
        this.registryHost = registryHost;

        this.zk = new ZooKeeper(this.registryHost, Constant.ZK_SESSION_TIMEOUT, e ->
                log.info("Connect to ZK server event", e));
        bootstrap();
    }

    private void bootstrap() {
        createPersistentZnode(Constant.REGISTRY_PATH, "".getBytes());
    }

    private void createPersistentZnode(String parentPath, byte[] data) {
        zk.create(parentPath,
                data,
                ZooDefs.Ids.OPEN_ACL_UNSAFE,
                CreateMode.PERSISTENT,
                createPersistentCallback,
                data);  // here I set the ctx as the data to write in the node, in case of retrying.
    }

    private StringCallback createPersistentCallback = new StringCallback() {
        @Override
        public void processResult(int rc, String path, Object ctx, String name) {
            switch (Code.get(rc)) {
                case CONNECTIONLOSS:
                    createPersistentZnode(path, (byte[]) ctx);
                    break;
                case OK:
                    log.info("Parent created, path: " + path + "with ctx: (" + ctx + ")");
                    break;
                case NODEEXISTS:
                    log.info("Node: " + path + " already exists.");
                    break;
                default:
                    log.error("Error creating parent path:",
                            KeeperException.create(Code.get(rc), path));
            }
        }
    };

    /* Init methods end*/

    @Override
    public void register(String interfaceName, String serverHost) {
        createPersistentZnode(Constant.REGISTRY_PATH + "/" + interfaceName, new byte[0]);
        createEphemeralZnode(Constant.REGISTRY_PATH + "/" + interfaceName + "/" + serverHost, "".getBytes());
    }

    private void createEphemeralZnode(String path, byte[] data) {
        zk.create(path, "".getBytes(), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL, createEphemeralCallback, path);
    }

    private StringCallback createEphemeralCallback = new StringCallback() {
        @Override
        public void processResult(int rc, String path, Object ctx, String name) {
            switch (Code.get(rc)) {
                case CONNECTIONLOSS:
                    createEphemeralZnode((String) ctx, "".getBytes());
                    break;
                case OK:
                    log.info("Node: " + path + " created");
                    break;
                case NODEEXISTS:
                    log.info("Node: " + path + " already exists.");
                    break;
                default:
                    log.error("Error creating ephemeral node", KeeperException.create(Code.get(rc), path));
            }
        }
    };

    @Override
    protected void shutdown() {
        try {
            this.zk.close();
        } catch (InterruptedException e) {
            log.error("InterruptedException on shutdown()", e);
            throw new RuntimeException(e);
        }
    }
}

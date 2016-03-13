package org.zstack.test;

import org.zstack.core.Platform;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.utils.ShellResult;
import org.zstack.utils.ShellUtils;
import org.zstack.utils.TimeUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.path.PathUtil;

import java.util.Properties;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

public class DBUtil {
    private static final CLogger logger = Utils.getLogger(DBUtil.class);
    
    public static void reDeployDB() {
        logger.info("Deploying database ...");
        String home = System.getProperty("user.dir");
        String baseDir = Utils.getPathUtil().join(home, "../");
        Properties prop = new Properties();
        
        try {
            prop.load(DBUtil.class.getClassLoader().getResourceAsStream("zstack.properties"));
            String user = prop.getProperty("DB.user");
            if (user == null) {
                user = prop.getProperty("DbFacadeDataSource.user");
            }
            if (user == null) {
                throw new CloudRuntimeException("cannot find DB user in zstack.properties, please set either DB.user or DbFacadeDataSource.user");
            }

            String password = prop.getProperty("DB.password");
            if (password == null) {
                password = prop.getProperty("DbFacadeDataSource.password");
            }
            if (password == null) {
                throw new CloudRuntimeException("cannot find DB user in zstack.properties, please set either DB.password or DbFacadeDataSource.password");
            }

            String shellcmd = String.format("build/deploydb.sh %s %s",  user, password);
            ShellUtils.run(shellcmd, baseDir, false);
            logger.info("Deploying database successfully");
        } catch (Exception e) {
            throw new CloudRuntimeException("Unable to deploy zstack database for testing", e);
        }
    }

    public static void reDeployCassandra(String keyspace) {
        // initializing platform causes zstack.properties to be load
        Platform.getUuid();
        logger.info("Redeploying cassandra");
        final String cqlsh = System.getProperty("Cassandra.cqlsh");
        if (cqlsh == null) {
            throw new RuntimeException("please set Cassandra.cqlsh in zstack.properties");
        }

        if (!PathUtil.exists(cqlsh)) {
            throw new RuntimeException(String.format("cannot find %s", cqlsh));
        }

        String cqlbin = System.getProperty("Cassandra.bin");
        if (cqlbin == null) {
            throw new RuntimeException("please set Cassandra.bin in zstack.properties");
        }

        if (!PathUtil.exists(cqlbin)) {
            throw new RuntimeException(String.format("cannot find %s", cqlbin));
        }

        ShellResult res = ShellUtils.runAndReturn(String.format("%s -e \"describe keyspaces\"", cqlsh), false);
        if (!res.isReturnCode(0)) {
            ShellUtils.run(String.format("bash -c %s &", cqlbin), false);
            TimeUtils.loopExecuteUntilTimeoutIgnoreException(120, 1, TimeUnit.SECONDS, new Callable<Boolean>() {
                @Override
                public Boolean call() throws Exception {
                    ShellResult res = ShellUtils.runAndReturn(String.format("%s -e \"describe keyspaces\"", cqlsh), false);
                    return res.isReturnCode(0);
                }
            });
        }

        ShellUtils.run(String.format("%s -e \"drop keyspace %s\"", cqlsh, keyspace), false);
    }
}


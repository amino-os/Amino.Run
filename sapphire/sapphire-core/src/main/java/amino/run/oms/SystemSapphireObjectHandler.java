package amino.run.oms;

import amino.run.app.SapphireObjectServer;
import amino.run.common.SystemSapphireObjectStatus;
import amino.run.kernel.common.GlobalKernelReferences;
import amino.run.kernel.server.KernelServerImpl;
import amino.run.policy.util.ResettableTimer;
import amino.run.sysSapphireObjects.SystemSapphireObjectList;
import java.net.InetSocketAddress;
import java.rmi.RemoteException;
import java.util.*;
import java.util.logging.Logger;

/** Class deploy System sapphire objects. */
public class SystemSapphireObjectHandler {
    private static Logger logger = Logger.getLogger(SystemSapphireObjectHandler.class.getName());
    private static String OPT_SEPARATOR = "=";
    private static String SO_SEPARATOR = ",";
    private static String SO_SPEC_SEPARATOR = ":";
    public static String EMPTY_SO_SPEC = "";
    private static int MAX_FAILED_RETRY = 50;
    private static int SYS_SO_INI_WAIT_TIME = 10000;
    private Map<String, SystemSODeployment> enabledSO = new HashMap<>();
    private KernelServerImpl localKernelServer;

    /** Construct default and enabled system sapphire object deploy. */
    SystemSapphireObjectHandler() {
        this.localKernelServer = GlobalKernelReferences.nodeServer;
    }

    private void parseSysSOList(String enabledSysSOList) {
        // create enabled system SO list
        String[] sysSOList = enabledSysSOList.split(OPT_SEPARATOR);
        if (sysSOList.length == 2) {
            String[] soList = sysSOList[1].split(SO_SEPARATOR);
            for (String so : soList) {
                String[] soAndSpec = so.split(SO_SPEC_SEPARATOR);
                if (soAndSpec.length == 0) {
                    continue; // ignore if pattern not followed
                }

                // ignore if SO does not exist
                String soName = soAndSpec[0];
                Object soDeploy = SystemSapphireObjectList.sysSOList.get(soName);
                if (soDeploy == null) {
                    logger.warning(
                            String.format("Supplied Sapphire Object [%s] does not exist", soName));
                    continue;
                }

                // add SO with Spec file
                switch (soAndSpec.length) {
                    case 1:
                        enabledSO.put(soName, new SystemSODeployment(soName, EMPTY_SO_SPEC));
                        break;
                    case 2:
                        enabledSO.put(soName, new SystemSODeployment(soName, soAndSpec[1]));
                        break;
                    default:
                        logger.warning("Sapphire Object list pattern not followed");
                }
            }
        }

        // add default sys sapphire object deployment
        for (String sysSO : SystemSapphireObjectList.sysDefaultSOList.keySet()) {
            Object sysSODeploy = enabledSO.get(sysSO);
            if (sysSODeploy != null) {
                continue; // default system SO is enabled already
            }

            enabledSO.put(sysSO, new SystemSODeployment(sysSO, EMPTY_SO_SPEC));
        }
    }

    /**
     * Schedule enabled and default system sapphire object to start. Method expect enabled system
     * sapphire object list in "--enableSysSO="SO1:Spec1,SO2:Spec2" format. SO is sapphire object
     * name and Spec is file path of spec file.
     *
     * @param enabledSysSOList enabled list of sapphire objects with respective spec
     */
    public void start(String enabledSysSOList) {
        if (enabledSysSOList != null) {
            parseSysSOList(enabledSysSOList);
        }

        SystemSODeployment systemSODeployment;
        for (Map.Entry<String, SystemSODeployment> systemSoList : enabledSO.entrySet()) {
            systemSODeployment = systemSoList.getValue();
            if (!systemSODeployment.isDeployed()) {
                systemSODeployment.waitAndStart();
            }
        }
    }

    /**
     * Return current status of system sapphire objects
     *
     * @return list of system sapphire object status
     */
    public List<SystemSapphireObjectStatus> getSystemSapphireObjectStatus() {
        List<SystemSapphireObjectStatus> statusList = new ArrayList<>();

        for (Map.Entry<String, SystemSODeployment> so : enabledSO.entrySet()) {
            statusList.add(new SystemSapphireObjectStatus(so.getKey(), so.getValue().isDeployed()));
        }
        return statusList;
    }

    class SystemSODeployment {
        private String soName;
        private String specFile;
        private int retry = 0;
        private long backoff = SYS_SO_INI_WAIT_TIME;
        private boolean deployed = false;
        private ResettableTimer timer;

        SystemSODeployment(String soName, String specFile) {
            this.soName = soName;
            this.specFile = specFile;
        }

        private void waitAndStart() {
            timer =
                    new ResettableTimer(
                            new TimerTask() {
                                public void run() {
                                    startSystemSO();
                                }
                            },
                            SYS_SO_INI_WAIT_TIME);
            timer.start();
        }

        private void startSystemSO() {
            // TODO: check kernel server availability as per SO spec
            if (!kernelServerAvailable()) {
                logger.warning("Kernel server not available for " + soName);
                timer.reset();
                return;
            }

            if (retry == MAX_FAILED_RETRY) {
                logger.warning(
                        String.format(
                                "System Sapphire object deployment failed with %d Max retry ",
                                MAX_FAILED_RETRY));
                timer.cancel();
                return;
            }

            // increment retry
            retry++;

            try {
                SystemSapphireObjectList.sysSOList
                        .get(soName)
                        .start((SapphireObjectServer) localKernelServer.oms, specFile);
            } catch (Exception e) {
                logger.warning("System sapphire object failed to start " + e);
                e.printStackTrace();

                backoff();
                timer.reset();
                return;
            }

            deployed = true;
            timer.cancel();
            timer = null;
            logger.info("System SO started :" + soName);
        }

        private void backoff() {
            // backoff with double time
            backoff = backoff * 2;
            timer =
                    new ResettableTimer(
                            new TimerTask() {
                                public void run() {
                                    startSystemSO();
                                }
                            },
                            backoff);
        }

        private boolean kernelServerAvailable() {
            try {
                List<InetSocketAddress> kernelServerList = localKernelServer.oms.getServers(null);
                return !kernelServerList.isEmpty();
            } catch (RemoteException e) {
                logger.warning("Failed to get kernel server list " + e);
                return false;
            }
        }

        private boolean isDeployed() {
            return deployed;
        }

        @Override
        public boolean equals(Object object) {
            if (!(object instanceof SystemSODeployment)) {
                return false;
            }
            return soName.equals(((SystemSODeployment) object).soName);
        }

        @Override
        public int hashCode() {
            return Objects.hash(soName);
        }
    }
}

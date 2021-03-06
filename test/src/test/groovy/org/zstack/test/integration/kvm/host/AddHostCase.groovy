package org.zstack.test.integration.kvm.host

import org.springframework.http.HttpEntity
import org.zstack.core.Platform
import org.zstack.core.cloudbus.CloudBus
import org.zstack.core.db.Q
import org.zstack.header.errorcode.SysErrors
import org.zstack.header.host.*
import org.zstack.header.message.MessageReply
import org.zstack.kvm.AddKVMHostMsg
import org.zstack.sdk.AddKVMHostAction
import org.zstack.sdk.ClusterInventory
import org.zstack.sdk.GetHypervisorTypesResult
import org.zstack.storage.primary.local.LocalStorageKvmBackend
import org.zstack.test.integration.kvm.KvmTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.HttpError
import org.zstack.testlib.SubCase
import org.zstack.utils.data.SizeUnit
import org.zstack.utils.gson.JSONObjectUtil
import org.zstack.utils.tester.ZTester

/**
 * Created by mingjian.deng on 2019/1/3.*/
class AddHostCase extends SubCase {
    EnvSpec env
    ClusterInventory cluster
    CloudBus bus

    @Override
    void setup() {
        useSpring(KvmTest.springSpec)
    }

    @Override
    void environment() {
        env = HostEnv.noHostBasicEnv()
    }

    @Override
    void clean() {
        env.delete()
    }

    @Override
    void test() {
        env.create {
            prepare()
            testCheckHostVersionFailure()
            testCheckHostManagementFailure()
            testInnerAddHostMsg()
            testGetHypervisorTypes()
            testAddHostFailureRollback()
        }
    }

    void prepare() {
        cluster = env.inventoryByName("cluster") as ClusterInventory
        bus = bean(CloudBus.class)
    }

    void testAddHostFailureRollback() {
        def initCalled = false
        def hangOnHostContinueConnectFlow = true

        env.afterSimulator(LocalStorageKvmBackend.INIT_PATH) { rsp, HttpEntity<String> e ->
            initCalled = true

            while (hangOnHostContinueConnectFlow) {
                sleep(1000)
            }

            return rsp
        }

        def res = null
        def hostUuid = Platform.uuid
        def addHostThread = Thread.start {
            def action = new AddKVMHostAction()
            action.sessionId = adminSession()
            action.resourceUuid = hostUuid
            action.clusterUuid = cluster.uuid
            action.managementIp = "127.0.0.3"
            action.name = "kvm"
            action.username = "root"
            action.password = "password"
            res = action.call()
        }

        def deleteHostThread = Thread.start {
            retryInSecs {
                assert initCalled
            }

            deleteHost {
                uuid = hostUuid
            }

            hangOnHostContinueConnectFlow = false
        }

        addHostThread.join()
        deleteHostThread.join()

        retryInSecs {
            assert res.error != null
        }
    }

    void testCheckHostVersionFailure() {
        env.testter.setNull(ZTester.KVM_HostVersion)
        def action = new AddKVMHostAction()
        action.sessionId = adminSession()
        action.resourceUuid = Platform.uuid
        action.clusterUuid = cluster.uuid
        action.managementIp = "127.0.0.2"
        action.name = "kvm"
        action.username = "root"
        action.password = "password"
        def res = action.call()

        assert res.error != null
        assert Q.New(HostVO.class).count() == 0
    }

    void testCheckHostManagementFailure() {
        env.testter.clearAll()

        def action = new AddKVMHostAction()
        action.sessionId = adminSession()
        action.resourceUuid = Platform.uuid
        action.clusterUuid = cluster.uuid
        action.managementIp = "###"
        action.name = "kvm"
        action.username = "root"
        action.password = "password"
        def res = action.call()

        assert res.error != null
        assert res.error.code == SysErrors.INVALID_ARGUMENT_ERROR.toString()
        assert Q.New(HostVO.class).count() == 0
    }

    void testInnerAddHostMsg() {
        env.testter.clearAll()

        AddKVMHostMsg amsg = new AddKVMHostMsg()
        amsg.accountUuid = loginAsAdmin().accountUuid
        amsg.name = "kvm"
        amsg.managementIp = "127.0.0.2"
        amsg.resourceUuid = Platform.uuid
        amsg.clusterUuid = cluster.uuid
        amsg.setPassword("password")
        amsg.setUsername("root")

        bus.makeLocalServiceId(amsg, HostConstant.SERVICE_ID)
        AddHostReply reply = (AddHostReply) bus.call(amsg)
        assert reply.inventory.status == HostStatus.Connected.toString()
    }

    void testGetHypervisorTypes() {
        GetHypervisorTypesResult result = getHypervisorTypes {
            sessionId = adminSession()
        }
        assert !result.getHypervisorTypes().isEmpty()
    }
}

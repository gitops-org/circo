/*
 * Copyright (c) 2012, the authors.
 *
 *    This file is part of Rush.
 *
 *    Rush is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation, either version 3 of the License, or
 *    (at your option) any later version.
 *
 *    Rush is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with Rush.  If not, see <http://www.gnu.org/licenses/>.
 */

package rush.ui
import akka.actor.Props
import akka.actor.UntypedActorFactory
import akka.testkit.JavaTestKit
import akka.testkit.TestActorRef
import rush.data.NodeData
import rush.messages.JobId
import test.ActorSpecification

import static test.TestHelper.addr

/**
 *
 * @author Paolo Di Tommaso <paolo.ditommaso@gmail.com>
 */
class TerminalUITest extends ActorSpecification {


    def "test renderScreen"   () {

        setup:
        final Props props = new Props( { new TerminalUIMock(dataStore, '1.1.1.1') } as UntypedActorFactory );
        final TestActorRef<TerminalUIMock> master = TestActorRef.create(system, props, "Terminal")

        def nodeData = new NodeData( address: addr('1.1.1.1') )
        def w1 = nodeData.createWorkerData( new JavaTestKit(system).getRef() )
        def w2 = nodeData.createWorkerData( new JavaTestKit(system).getRef() )
        def w3 = nodeData.createWorkerData( new JavaTestKit(system).getRef() )
        def w4 = nodeData.createWorkerData( new JavaTestKit(system).getRef() )

        nodeData.processed = 32
        nodeData.failed = 6

        w1.with {
            currentJobId = JobId.of('123')
            processed = 123
            failed = 3
        }

        w2.with {
            currentJobId = JobId.of('555')
            processed = 943
            failed = 76
        }

        w3.with {
            currentJobId = JobId.of('6577')
            processed = 7843
            failed = 111
        }

        dataStore.putNodeData(nodeData)

        when:
        def screen = master.underlyingActor().renderScreen()

        print(screen)

        then:
        noExceptionThrown()

    }





}

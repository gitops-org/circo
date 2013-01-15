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

package rush.data

import com.hazelcast.core.EntryEvent
import com.hazelcast.core.EntryListener
import com.hazelcast.core.Hazelcast
import com.hazelcast.core.HazelcastInstance
import com.hazelcast.core.IMap
import com.hazelcast.query.SqlPredicate
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import rush.messages.JobEntry
import rush.messages.JobStatus

import java.util.concurrent.locks.Lock
/**
 *
 * @author Paolo Di Tommaso <paolo.ditommaso@gmail.com>
 */

@Slf4j
@CompileStatic
class HazelcastDataStore extends AbstractDataStore {

    private HazelcastInstance hazelcast

   private Map<Closure,EntryListener> listenersMap = [:]

    def HazelcastDataStore( HazelcastInstance instance = null ) {

        if( !instance ) {
            log.warn "Using TEST Hazelcast instance"
            instance = Hazelcast.newHazelcastInstance(null)
        }

        hazelcast = instance
        //idGen = hazelcast.getAtomicNumber('idGen')
        jobsMap = hazelcast.getMap('store')
        nodeDataMap = hazelcast.getMap('nodeInfo')
    }


    @Override
    protected Lock getLock(def key) {
        hazelcast.getLock(key)
    }

    List<JobEntry> findJobsById( final String jobId) {
        assert jobId

        String ticket
        String index
        int pos = jobId.indexOf(':')
        if( pos == -1 ) {
            ticket = jobId
            index = null
        }
        else {
            String[] slice = jobId.split('\\:')
            ticket = slice[0]
            index = slice.size()>1 ? slice[1] : null
        }

        // replace the '*' with sql '%'
        ticket = ticket.replaceAll('\\*','%')
        if ( index ) {
            index = index.replaceAll('\\*','%')
        }

        if ( !ticket.contains('%') ) {
            ticket += '%'
        }


        def match = index ? "$ticket:$index" : ticket
        def criteria = "id.toString() LIKE '$match'"

        def result = (jobsMap as IMap) .values(new SqlPredicate(criteria))
        new ArrayList<JobEntry>(result as Collection<JobEntry>)
    }


    List<JobEntry> findJobsByStatus( JobStatus[] status ) {
        assert status

        def criteria = new SqlPredicate("status IN (${status.join(',')})  ")
        def result = (jobsMap as IMap) .values(criteria)
        new ArrayList<JobEntry>(result as Collection<JobEntry>)

    }

    void addNewJobListener(Closure listener) {
        assert listener

        def entry = new EntryListener() {
            @Override
            void entryAdded(EntryEvent event) { listener.call(event.getValue()) }

            @Override
            void entryRemoved(EntryEvent event) { }

            @Override
            void entryUpdated(EntryEvent event) { }

            @Override
            void entryEvicted(EntryEvent event) { }
        }

        (jobsMap as IMap) .addLocalEntryListener(entry)
        listenersMap.put(listener, entry)
    }


    void removeNewJobListener( Closure listener ) {
        def entry = listenersMap.get(listener)
        if ( !entry ) { log.warn "No listener registered for: $listener"; return }
        (jobsMap as IMap).removeEntryListener(entry)
    }



}

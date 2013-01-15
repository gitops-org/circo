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

package rush.client.cmd
import com.beust.jcommander.DynamicParameter
import com.beust.jcommander.Parameter
import com.beust.jcommander.Parameters
import groovy.transform.ToString
import groovy.util.logging.Slf4j
import rush.client.ClientApp
import rush.frontend.CmdSubResponse
import rush.messages.JobReq
import scala.concurrent.duration.Duration
/**
 *
 * @author Paolo Di Tommaso <paolo.ditommaso@gmail.com>
 */

@Slf4j
@ToString(includePackage = false)
@Parameters(commandNames='sub', commandDescription='Submit a new job for cluster execution')
class CmdSub extends AbstractCommand  {

    @Parameter
    List<String> command

    @Parameter(names = ['-sync','--sync'], description = "Causes the 'sub' command to wait for  the  job  to  complete before  exiting")
    boolean sync

    @DynamicParameter(names = "-v", description = "Expands the list of environment variables that are exported to the job")
    Map<String, String> env = new HashMap<String, String>();

    @Parameter(names = '-V', description = 'Declares that all environment variables in the qsub commands environment are to be exported to the batch job')
    boolean exportAllEnvironment

    @Parameter(names=['--max-duration'], description = 'Max duration allows for the job (sec|min|hour|day)', converter = DurationConverter)
    Duration maxDuration

    @Parameter(names=['--max-attempts'], description = 'Max number of times the job can be resubmitted in case of error')
    int maxAttempts = 2

    @Parameter(names=['--max-inactive'], description='Max allowed period of job inactivity (sec|min|hour|day)', converter = DurationConverter)
    Duration maxInactive = Duration.create('10 min')

    @Parameter(names='--print', description = 'Wait for the job completion and print out the result to the stdout')
    boolean printOutput

    /**
     * The command will be submitted the number of times specified the range
     *
     * note: due to a bug of JCommand this field has to be declared as Object
     * see https://groups.google.com/d/topic/jcommander/_p4Jl_c4PO0/discussion
     *
     */
    @Parameter(names=['-t','--times'], arity = 1, description = 'Number of times this request has to be submitted', converter = IntRangeConverter)
    private times

    def Range getTimes() {  times as Range }



    private initEnv() {
        if( exportAllEnvironment ) {
            def copy = new HashMap<>(env)
            env = new HashMap<>( System.getenv() )
            if( copy ) {
                env.putAll(copy)
            }
        }
    }

    /**
     * @return Convert this job submit command to a valid {@code JobReq} instance
     */
    JobReq createJobReq() {
        assert command

        def result = new JobReq()
        result.environment = new HashMap<>(env)
        result.script = command.join(' ')
        result.maxAttempts = maxAttempts
        if ( maxDuration ) {
            result.maxDuration = maxDuration.toMillis()
        }
        if ( maxInactive ) {
            result.maxInactive = maxInactive.toMillis()
        }

        return result
    }




    @Override
    void execute(ClientApp client) {

        initEnv()

        if( !command ) {
            println "no command to submit provided -- nothing to do"
            return
        }

        CmdSubResponse result = client.send(this)


    }

}

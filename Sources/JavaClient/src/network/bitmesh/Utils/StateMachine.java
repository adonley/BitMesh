package network.bitmesh.Utils;

import org.slf4j.LoggerFactory;

/**
 * Created by christopher on 8/14/15. This holds the code that got duplicated a lot in all of our state machines.
 * See PaymentStateMachine for an example implementation
 */
public abstract class StateMachine
{
    public static final org.slf4j.Logger log = LoggerFactory.getLogger(StateMachine.class);

    public StateMachine() {}

    public interface State {}

    public StateMachine(State[] states, State initial)
    {
        this.states = states;
        this.currentState = initial;
        throwsOnIllegalBadTransition = false;
    }

    public State currentState;
    public State[] states;
    public boolean throwsOnIllegalBadTransition;

    // Define edges to state machine here
    public abstract boolean transitionIsValid(State newState);

    public void setState(State newState) throws IllegalStateException
    {
        if (!transitionIsValid(newState))
        {
            log.error("Illegal state transition from {} to {}", currentState, newState);
            if (throwsOnIllegalBadTransition)
            {
                throw new IllegalStateException();
            }
        }
        this.currentState = newState;
    }

    public State getState() { return currentState; }
}

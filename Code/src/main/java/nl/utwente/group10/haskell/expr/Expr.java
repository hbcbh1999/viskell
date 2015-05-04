package nl.utwente.group10.haskell.expr;

import java.util.logging.Logger;

import nl.utwente.group10.haskell.HaskellObject;
import nl.utwente.group10.haskell.env.Env;
import nl.utwente.group10.haskell.exceptions.HaskellException;
import nl.utwente.group10.haskell.hindley.GenSet;
import nl.utwente.group10.haskell.type.Type;

/**
 * An expression in Haskell.
 */
public abstract class Expr extends HaskellObject {
    /** Logger for this class. **/
    protected static final Logger logger = Logger.getLogger(Expr.class.getName());

    /**
     * Analyzes the type tree and resolves the type for this usage of this expression.
     *
     * @param env The current Haskell environment.
     * @param genSet TODO Find out what this does...
     * @return The type for this usage of this expression.
     * @throws HaskellException The type tree contains an application of an incompatible type.
     */
    public abstract Type analyze(final Env env, final GenSet genSet) throws HaskellException;

    /**
     * Analyzes the type tree and resolves the type for this usage of this expression, using an empty GenSet. This
     * method can be used to call when analyzing the root of the expression tree (the first step in the type inference
     * of an expression).
     *
     * @param env The current Haskell environment.
     * @return The type for this usage of this expression.
     * @throws HaskellException The type tree contains an application of an incompatible type.
     */
    public Type analyze(final Env env) throws HaskellException {
        return this.analyze(env, new GenSet());
    }

    /**
     * Returns the Haskell code for this expression.
     * @return The Haskell code for this expression.
     */
    public abstract String toHaskell();

    /**
     * @return A string representation of this Haskell expression.
     */
    @Override
    public abstract String toString();
}

package nl.utwente.group10.ui.components.blocks;

import nl.utwente.group10.haskell.env.Env;
import nl.utwente.group10.haskell.exceptions.HaskellException;
import nl.utwente.group10.haskell.hindley.GenSet;
import nl.utwente.group10.haskell.type.ConstT;
import nl.utwente.group10.haskell.type.Type;
import nl.utwente.group10.ui.components.anchors.OutputAnchor;

public interface OutputBlock {

    /**
     * @return the output Anchor for this Block
     */
    public OutputAnchor getOutputAnchor();

    /*
     * Signature = non unified type, ie: a->b
     * 
     * (Current)Type = unified type, ie Int -> Float (This can still have
     * signature a->b)
     * 
     * These are not the same, but are related. The Type has to conform to the
     * signature.
     */
    
    /**
     * @return The current output type of the block.
     */
    public Type getOutputType();

    public Type getOutputType(Env env);

    /**
     * @return The output type as specified by the function's signature.
     */
    public Type getOutputSignature();

    public Type getOutputSignature(Env env);
}
package nl.utwente.group10.ui.components.blocks.function;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.fxml.FXML;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import nl.utwente.group10.haskell.exceptions.HaskellException;
import nl.utwente.group10.haskell.expr.Apply;
import nl.utwente.group10.haskell.expr.Expr;
import nl.utwente.group10.haskell.expr.Ident;
import nl.utwente.group10.haskell.type.FuncT;
import nl.utwente.group10.haskell.type.Type;
import nl.utwente.group10.ui.BackendUtils;
import nl.utwente.group10.ui.CustomUIPane;
import nl.utwente.group10.ui.components.anchors.InputAnchor;
import nl.utwente.group10.ui.components.anchors.OutputAnchor;
import nl.utwente.group10.ui.components.blocks.Block;
import nl.utwente.group10.ui.components.blocks.input.InputBlock;
import nl.utwente.group10.ui.components.blocks.output.OutputBlock;
import nl.utwente.group10.ui.exceptions.FunctionDefinitionException;
import nl.utwente.group10.ui.handlers.ConnectionCreationManager;

/**
 * Main building block for the visual interface, this class represents a Haskell
 * function together with it's arguments and visual representation.
 */
public class FunctionBlock extends Block implements InputBlock, OutputBlock {
    /** The OutputAnchor of this FunctionBlock. */
    private OutputAnchor output;

    /** The function name. */
    private StringProperty name;

    /** The space containing the input anchor(s). */
    @FXML private Pane inputSpace;

    /** The space containing the output anchor. */
    @FXML private Pane outputSpace;

    /** The space containing all the arguments of the function. */
    private ArgumentSpace argumentSpace;

    /** The space containing the output anchor of this FunctionBLock. */
    @FXML private Pane outputTypesSpace;

    /**
     * Method that creates a newInstance of this class along with it's visual
     * representation
     *
     * @param name
     *            The name of the function.
     * @param type
     *            The function's type (usually a FuncT).
     * @param pane
     *            The parent pane in which this FunctionBlock exists.
     */
    public FunctionBlock(String name, Type type, CustomUIPane pane) {
        super(pane);
        this.name = new SimpleStringProperty(name);

        this.loadFXML("FunctionBlock");

        // Collect argument types
        ArrayList<String> args = new ArrayList<>();
        Type t = type;
        while (t instanceof FuncT) {
            FuncT ft = (FuncT) t;
            args.add(ft.getArgs()[0].toHaskellType());
            t = ft.getArgs()[1];
        }
        
        List<Type> inputSignatures = new ArrayList<>();
        Type functionSignature = getFunctionSignature();
        for (int i = 0; i < args.size(); i++) {
            if (functionSignature instanceof FuncT) {
                Type inputSignature = BackendUtils.dive((FuncT) functionSignature, i + 1);
                inputSignatures.add(inputSignature);
            } else {
                throw new FunctionDefinitionException();
            }
        }
        
        argumentSpace = new ArgumentSpace(this, inputSignatures);
        argumentSpace.setKnotIndex(getAllInputs().size());
        
        ((Pane) this.lookup("#nestSpace")).getChildren().add(argumentSpace);
        argumentSpace.knotIndexProperty().addListener(e -> invalidateKnotIndex());
        
        // Create an anchor for the result
        output = new OutputAnchor(this);
        output.layoutXProperty().bind(outputSpace.widthProperty().divide(2));
        outputSpace.getChildren().add(output);
        
        // Make sure the prefWidth is correctly updated.
        Pane functionInfo = (Pane) this.lookup("#functionInfo");
        this.prefWidthProperty().bind(functionInfo.widthProperty().add(argumentSpace.prefWidthProperty()));
        
        
        this.setMinSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        this.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        functionInfo.setMinWidth(Region.USE_PREF_SIZE);
        functionInfo.setMaxWidth(Region.USE_PREF_SIZE);
        invalidateConnectionState();
    }
    
    /** Returns the name property of this FunctionBlock. */
    public final String getName() {
        return name.get();
    }

    /** Sets the name property of this FunctionBlock. */
    public void setName(String name) {
        this.name.set(name);
    }
    
    /** Returns the StringProperty for the name of the function. */
    public final StringProperty nameProperty() {
        return name;
    }

    /** Returns the knot index of this FunctionBlock. */
    public final Integer getKnotIndex() {
        return argumentSpace.knotIndexProperty().get();
    }

    /**
     * @param index The new knot index for this FunctionBlock.
     */
    public final void setBowtieIndex(int index) {
        if (index >= -1 && index <= getAllInputs().size()) {
            argumentSpace.setKnotIndex(index);
        } else {
            throw new IndexOutOfBoundsException();
        }
    }

    @Override
    public final List<InputAnchor> getAllInputs() {
        return argumentSpace.getInputArguments().stream().map(a -> a.getInputAnchor()).collect(Collectors.toList());
    }

    /**
     * @return Only the active (as specified by the knot index) inputs.
     */
    @Override
    public List<InputAnchor> getActiveInputs() {
        return getAllInputs().subList(0, getKnotIndex());
    }

    /**
     * @param index Index of the InputAnchor to return.
     * @return InputAnchor with the given index.
     */
    public InputAnchor getInput(int index) {
        return argumentSpace.getInputArgument(index).getInputAnchor();
    }

    @Override
    public OutputAnchor getOutputAnchor() {
        return output;
    }

    /**
     * @return The function signature as specified in the catalog.
     */
    public Type getFunctionSignature() {
        return getPane().getEnvInstance().getFreshExprType(this.getName()).get();
    }

    @Override
    public Type getInputSignature(int index) {
        return getInput(index).getSignature();
    }
    
    @Override
    public Type getInputType(int index) {
        return getInput(index).getType();
    }
    
    @Override
    public Type getOutputSignature() {
        Type type = getFunctionSignature();
        for (int i = 0; i < getKnotIndex(); i++) {
            if (type instanceof FuncT) {
                type = ((FuncT) type).getArgs()[1];
            } else {
                throw new FunctionDefinitionException();
            }
        }
        return type;
    }

    @Override
    public Type getOutputType() {
        try {
            return asExpr().analyze(getPane().getEnvInstance()).prune();
        } catch (HaskellException e) {
            return getOutputSignature();
        }
    }

    /**
     * @return The current (output) expression belonging to this block.
     */
    @Override
    public final Expr asExpr() {
        Expr expr = new Ident(getName());
        for (InputAnchor in : getActiveInputs()) {
            expr = new Apply(expr, in.asExpr());
        }

        return expr;
    }
    
    @Override
    public void invalidateConnectionState() {
        invalidateInputVisuals();
        invalidateOutputVisuals();
    }

    /**
     * Updates the input types to the Block's new state.
     */
    private void invalidateInputVisuals() {
        argumentSpace.invalidateInputContent();
    }

    /**
     * Updates the output types to the Block's new state.
     */
    private void invalidateOutputVisuals() {
        argumentSpace.invalidateOutputContent();
    }

    /**
     * React to a potential state change with regards to the knot index.
     * This then activates / disables the inputs with regards to the knot index.
     */
    private void invalidateKnotIndex() {
        List<InputAnchor> inputs = getAllInputs();
        for (int i = 0; i < inputs.size(); i++) {
            inputs.get(i).setActive(i < getKnotIndex());
        }
        
        // Trigger invalidation for the now changed output type.
        ConnectionCreationManager.nextConnectionState();
        invalidateConnectionStateCascading();
    }
    
    @Override
    public String toString() {
        return this.getName();
    }
}

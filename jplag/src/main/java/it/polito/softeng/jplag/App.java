package it.polito.softeng.jplag;

import com.github.gumtreediff.actions.model.Action;
import gumtree.spoon.AstComparator;
import gumtree.spoon.diff.Diff;
import gumtree.spoon.diff.operations.Operation;
import spoon.reflect.cu.SourcePosition;
import spoon.reflect.declaration.CtElement;

import java.io.File;
import java.util.List;

public class App {

    public static void main(String[] args) throws Exception {

        File fSource = new File(args[0]);
        File fTarget = new File(args[1]);

        Diff diff = new AstComparator().compare(fSource, fTarget);
        List<Operation> operations = diff.getAllOperations();
        for(Operation operation : operations){
            //System.out.println(operation);
            CtElement srcNode = operation.getSrcNode();
            CtElement dstNode = operation.getDstNode();
            if(srcNode != null && dstNode != null){
                Action action = operation.getAction();
                String actionName = action.getName();
                SourcePosition srcPosition = srcNode.getPosition();
                SourcePosition dstPosition = dstNode.getPosition();
                int sS = srcPosition.getLine();
                int dS = dstPosition.getLine();
                System.out.println(actionName + ": " + sS + " - " + dS);
            }


        }


    }

}

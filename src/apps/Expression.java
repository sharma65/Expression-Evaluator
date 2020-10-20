package apps;

import java.io.*;
import java.util.*;

import structures.Stack;

public class Expression {

	/**
	 * Expression to be evaluated
	 */
	String expr;  

	/**
	 * Scalar symbols in the expression 
	 */
	ArrayList<ScalarSymbol> scalars;   
	
	/**
	 * Array symbols in the expression
	 */
	ArrayList<ArraySymbol> arrays;
    /**
     * String containing all delimiters (characters other than variables and constants), 
     * to be used with StringTokenizer
     */
    public static final String delims = " \t*+-/()[]";
    
    private String modExpr;
	private int count;
	private ArrayList<Integer> openingLocations;
	private ArrayList<Integer> closingLocations;
    
    /**
     * Initializes this Expression object with an input expression. Sets all other
     * fields to null.
     * 
     * @param expr Expression
     */
    public Expression(String expr) {
        this.expr = expr;
    }

    
    
    private void setLocations(String expr){
    	
    	openingLocations = new ArrayList<Integer>();
    	closingLocations = new ArrayList<Integer>();
    	Stack <Integer> fixLoc = new Stack<Integer>();
    	
    	int count = 0;	
    	
    	for (int i = 0; i < expr.length(); i++){
    		if(expr.charAt(i) == ')' || expr.charAt(i) == ']')
    			count++;
    	}
    	
    	for (int i = 0; i < count; i++)
    		closingLocations.add(0);
    	
    	count = 0;

    	for (int i = 0; i < expr.length(); i++){

    		if (expr.charAt(i) == '(' || expr.charAt(i) == '['){
    			openingLocations.add(i);
    			fixLoc.push(count);
    			count++;
    			
    		}else if (expr.charAt(i) == ')' || expr.charAt(i) == ']'){

    			closingLocations.set(fixLoc.pop(), i);
    		}
    			
    	}
    }
    /**
     * Populates the scalars and arrays lists with symbols for scalar and array
     * variables in the expression. For every variable, a SINGLE symbol is created and stored,
     * even if it appears more than once in the expression.
     * At this time, values for all variables are set to
     * zero - they will be loaded from a file in the loadSymbolValues method.
     */
    public void buildSymbols() {

    	scalars = new ArrayList<ScalarSymbol>();
    	arrays = new ArrayList<ArraySymbol>();
    	Stack <String> temp = new Stack<String>();
    	    	
    	StringTokenizer symTokenizer = new StringTokenizer(expr, delims, true);
    	
    	String curr = "";
    	
    	while (symTokenizer.hasMoreTokens()){
    		curr = symTokenizer.nextToken();
    		if(Character.isLetter(curr.charAt(0)) || curr.equals("["))
    			temp.push(curr);	
    	}
    	
    	while (!temp.isEmpty()){
    		
    		curr = temp.pop();
    		if (curr.equals("[")){
    			curr = temp.pop();
    			ArraySymbol as = new ArraySymbol(curr);
    			if(arrays.indexOf(as) == -1)
    				arrays.add(as);
    		}else{
    			ScalarSymbol ss = new ScalarSymbol(curr);
    			if(scalars.isEmpty() || !scalars.contains(ss))
    				scalars.add(ss);
    		}
    	}
    	
    }
    
    /**
     * Loads values for symbols in the expression
     * 
     * @param sc Scanner for values input
     * @throws IOException If there is a problem with the input 
     */
    public void loadSymbolValues(Scanner sc) 
    throws IOException {
        while (sc.hasNextLine()) {
            StringTokenizer st = new StringTokenizer(sc.nextLine().trim());
            int numTokens = st.countTokens();
            String sym = st.nextToken();
            ScalarSymbol ssymbol = new ScalarSymbol(sym);
            ArraySymbol asymbol = new ArraySymbol(sym);
            int ssi = scalars.indexOf(ssymbol);
            int asi = arrays.indexOf(asymbol);
            if (ssi == -1 && asi == -1) {
            	continue;
            }
            int num = Integer.parseInt(st.nextToken());
            if (numTokens == 2) { // scalar symbol
                scalars.get(ssi).value = num;
            } else { // array symbol
            	asymbol = arrays.get(asi);
            	asymbol.values = new int[num];
                // following are (index,val) pairs
                while (st.hasMoreTokens()) {
                    String tok = st.nextToken();
                    StringTokenizer stt = new StringTokenizer(tok," (,)");
                    int index = Integer.parseInt(stt.nextToken());
                    int val = Integer.parseInt(stt.nextToken());
                    asymbol.values[index] = val;              
                }
            }
        }
    }
    

    /**
     * Evaluates the expression, using RECURSION to evaluate subexpressions and to evaluate array 
     * subscript expressions.
     * 
     * @return Result of evaluation
     */
    public float evaluate() {
    	
    	String newExp = "";
		for (int i = 0; i < expr.length(); i++){
			if(expr.charAt(i) != ' ')
				newExp += expr.charAt(i);
		}
		
		expr = newExp;
		modExpr = expr;
		count = 0;
		setLocations(expr);
    	return evaluate(expr, expr.length()-1); 	
    }
    
    private float evaluate(String expr, int index){
    			
    	Stack <String> oStack = new Stack <String>();
        Stack <Float> fStack = new Stack <Float>();
        StringTokenizer expTokenizer = new StringTokenizer(expr, delims, true);
        String temp = "", varName = "";
        int arrIndex = 0;
        float subEval = 0, sValue = 0, cValue = 0;
        
        while (expTokenizer.hasMoreTokens()){
        	
        	temp = expTokenizer.nextToken();
        	
        	if (temp.equals("(") || temp.equals("[")){
        		
        		int x = openingLocations.get(count);
        		int y = closingLocations.get(count);
        		count++;
        		subEval = evaluate(modExpr.substring(x+1, y), y-1);
        		
	        	if (temp.equals("[")){
	        		
	        		for (int i = 0; i < arrays.size(); i++){
	        			if (arrays.get(i).name.equals(varName))
	        				 arrIndex = i;
	        		}
	        		
	        		int [] arrTemp = arrays.get(arrIndex).values;
	        		subEval = arrTemp[(int)subEval];
	        		fStack.push(subEval);
	        		if (!oStack.isEmpty()){
	        			if (oStack.peek().equals("/") || oStack.peek().equals("*"))
	        				fStack.push(calculate(oStack, fStack, 0));	
	        		}
	        	}else
	        		fStack.push(subEval);
	        		if (!oStack.isEmpty()){
	        			if (oStack.peek().equals("/") || oStack.peek().equals("*"))
	        				fStack.push(calculate(oStack, fStack, 0));	
	        		}
	        	
	        	if (y == index)
	        		break;
	        	else
	        		expTokenizer = new StringTokenizer(modExpr.substring(y+1, index+1), delims, true);	
        	}else if(Character.isLetter(temp.charAt(0))){
        		
        		varName = temp;
        		ArraySymbol as = new ArraySymbol(temp);
	        	if (!arrays.contains(as)){
	        		ScalarSymbol ss = new ScalarSymbol(temp);
	        		int ssi = scalars.indexOf(ss);
	        		sValue = scalars.get(ssi).value;
	        		fStack.push(sValue);
	        		if (!oStack.isEmpty()){
	        			if (oStack.peek().equals("/") || oStack.peek().equals("*"))
	        				fStack.push(calculate(oStack, fStack, 0));	
	        		}
        		}
        		
        	}else if(temp.equals("+") || temp.equals("-") || temp.equals("/") || temp.equals("*")){
        		oStack.push(temp);
        	}else if(Character.isDigit(temp.charAt(0))){
        		
        		cValue = (float) Integer.parseInt(temp.trim());
        		fStack.push(cValue);
        		if (!oStack.isEmpty()){
        			if (oStack.peek().equals("/") || oStack.peek().equals("*"))
        				fStack.push(calculate(oStack, fStack, 0));	
        		}
        	}
        	
        }
        
        if (oStack.isEmpty())
        	return fStack.pop();
        
        Stack <Float> infix = new Stack <Float>();
        Stack <String> opfix = new Stack <String>();
        while(!fStack.isEmpty())
        	infix.push(fStack.pop());
        while(!oStack.isEmpty())
        	opfix.push(oStack.pop());
        while(!opfix.isEmpty())
        	infix.push(calculate(opfix, infix, 1));;
        return infix.pop();
        
    }
    
    private float calculate(Stack <String> oStack, Stack <Float> fStack, int x){
    	
    	float a, b, c = 0;
    	
    	if (x == 0){
    		b = fStack.pop();
    		a = fStack.pop();
    	}else{
    		a = fStack.pop();
    		b = fStack.pop();
    	}

    	String temp = oStack.pop();
    	if (temp.equals("/"))
    		c = a/b;  
    	else if (temp.equals("*"))
    		c = a*b;
    	else if (temp.equals("+")){
    		if(!oStack.isEmpty() && (oStack.peek().equals("*") || oStack.peek().equals("/"))){
    			c = a + calculate(oStack, fStack, 0);
    		}else
    			c = a + b;
    	}else if (temp.equals("-")){
    		if(!oStack.isEmpty() && (oStack.peek().equals("*") || oStack.peek().equals("/"))){
    			c = a + calculate(oStack, fStack, 0);
    		}else
    			c = a - b;
    	}
    	return c;
    	
    }
    /**
     * Utility method, prints the symbols in the scalars list
     */
    public void printScalars() {
        for (ScalarSymbol ss: scalars) {
            System.out.println(ss);
        }
    }
    
    /**
     * Utility method, prints the symbols in the arrays list
     */
    public void printArrays() {
    		for (ArraySymbol as: arrays) {
    			System.out.println(as);
    		}
    }

}
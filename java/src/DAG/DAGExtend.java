package DAG;

import multicore_exp.DAGTest;
import java.util.concurrent.ThreadLocalRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

public class DAGExtend{

	public static int gen_mode = 2;
	public static int num_node = 7;
	public static int num_edge =(num_node-2)*(num_node-1)/6;
	public static int group_mode = 2;
	public static boolean ng_print = false;
	public static int NUM_CORES = 1;
	

	public static void main(String args[]) {
		
		
		/*stack of Node(s) to hold fork/join ranking info*/
		Stack<Node> jfNodes = new Stack<Node>();
		
		
		
		Node topology = Dag.random_generate2(num_node, num_edge); 
		DAGTest t = new DAGTest(num_node); 
		
		drawDag(topology);
		
		
	}
	
	public static Stack<Node> findJoinForks(Node node)
	{
		ArrayList<Node> next = node.getNext();
		Node cur = node;
		Stack<Node> s = new Stack<Node>();
		if(next.size()==0)
			return null;
		while(cur.visit == false)
		{
			cur.visit = true;
			if(cur.getNext().size() > 1 || cur.getPrev().size() > 1)
			{
				s.push(cur);
			}
		}
		for (int i = 0; i < next.size(); i++)
		{
			/*this structure clearly wont work because stack will be
			 * created and destroyed over and over */

			//findJoinForks(next.get(i));
		}
		
	}
	
	public static void drawDag(Node node)
	{
		ArrayList<Node> nexts = node.getNext();

		if(nexts.size()==0)
			return;

		if(node.visit == true)
			return;
		node.visit = true;

		System.out.print(node.getId() + "'d" + node.depth + "'" + "--");
		for(int i = 0; i < nexts.size(); ++i)
		{
			System.out.print(nexts.get(i).getId() + "(" + nexts.get(i).depth + ") ");
		}

		System.out.println();
		for(int i = 0; i < nexts.size(); ++i)
			drawDag(nexts.get(i));
	}
	
	public static ArrayList<ArrayList<Integer>> trans_dag (NodeGroup dag)
	{
		ArrayList<ArrayList<Integer>> temp = new ArrayList<ArrayList<Integer>>();
		
		NodeGroup cur = dag;
		while (cur != null)
		{
			ArrayList<Integer> t = new ArrayList<Integer>();
			ArrayList<Node> nodes = cur.getNodes();

			for(int i=0; i<nodes.size(); ++i)
				t.add(nodes.get(i).getId());

			temp.add(t);
			cur = cur.getNext();
		}

		return temp;
	}
}
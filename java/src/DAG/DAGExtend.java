package DAG;

import multicore_exp.DAGTest;
import java.util.concurrent.ThreadLocalRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
import java.util.*;

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
	
	/*This code takes the first node in graph and adds it to a queue.
	 * While the queue is not empty, it will look at each node in the queue
	 * and determine if that node has multiple previous or multiple next nodes,
	 * if it does, it will then add that node to a stack. It will then get the
	 * list of next nodes from the current node that was in the queue and 
	 * add all of those nodes to the queue to be explored. By the end of the
	 * while loop, all nodes should have been examined and all fork and join
	 * nodes will have been added to stack which is returned to calling function.
	 * I employed a BFS algorithm for looking at this graph. Refer to wikipedia if confused*/
	public static Stack<Node> findJoinForks(Node node)
	{
		Node cur = node;
		cur.visit = true;
		Stack<Node> s = new Stack<Node>();
		Queue<Node> q = new LinkedList<Node>();
		q.add(cur);
		while(!q.isEmpty()) {
			Node t = q.remove();
			ArrayList<Node> tnext = t.getNext();
			if(tnext.size() > 1 || t.getPrev().size() > 1) {
				s.push(t);
			}
			for(int i = 0; i < tnext.size(); i++) {
				Node o = tnext.get(i);
				if(o.visit != true) {
					o.visit = true;
					q.add(o);
				}
			}
		}
		
		return s;
		
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
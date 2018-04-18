package DAG;

import multicore_exp.DAGTest;
import java.util.concurrent.ThreadLocalRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
import java.util.*;

public class DAGExtend{

	public static int gen_mode = 2;
	public static int num_node = 10;
	public static int num_edge =(num_node-2)*(num_node-1)/6;
	public static int group_mode = 2;
	public static boolean ng_print = false;
	public static int NUM_CORES = 1;


	public static void main(String args[]) {
		parseArgs(args);

		/*stack of Node(s) to hold fork/join ranking info*/
		Stack<Node> jfNodes = new Stack<Node>();

		ArrayList<Integer> test = new ArrayList<Integer>();



		Node topology = Dag.random_generate2(num_node, num_edge);
		

		drawDag(topology);
		resetDag(topology);

		ArrayList<ArrayList<Integer>> blocks = new ArrayList<ArrayList<Integer>>();
		ArrayList<ArrayList<Integer>> naive = new ArrayList<ArrayList<Integer>>();
		
		
		

		arrangeByPrecCons(topology, blocks);
		naive = buildNaive(blocks);
		
		System.out.println();

		printBlocks(blocks);
		System.out.println();
		printBlocks(naive);
		
		System.out.println();
		
		NodeGroup blockedNodeGroup = transformToNodeGroup(blocks);
		NodeGroup naiveNodeGroup = transformToNodeGroup(naive);
		//Dag.DrawNodeGroup(blockedNodeGroup);
		System.out.println();
		
		
		double td = 0;
		double tn = 0;
		double d = 0;
		double n = 0;
		int seed = 0;
		int validTaskCount = 0;
		
		double tempNaive = 0.0;
		double tempBlock = 0.0;
		
		DAGTest t = new DAGTest(num_node);
		
		//ArrayList<ArrayList<Integer>> blockedNodeGroupTrans = trans_dag(blockedNodeGroup);
		//ArrayList<ArrayList<Integer>> naiveNodeGroupTrans = trans_dag(naiveNodeGroup);
		
		for(int i = 0; i < 100; i++)
		{
			seed = i;
			
			tempNaive = t.getDensity(seed, naive);
			tempBlock = t.getDensity(seed, blocks);
			
			//We want to ignore situations in which density goes to infinity, however it is worth visiting later, because it is more proof that our approach is better than naive approach
			if(tempNaive != Double.POSITIVE_INFINITY && tempBlock != Double.POSITIVE_INFINITY)
			{
				d += t.getDensity(seed, blocks);
				n += t.getDensity(seed, naive);
				validTaskCount++;
			}
			
		}
		d = d / validTaskCount++;
		n = n / validTaskCount++;
		
		System.out.println("Heuristsic value");
		System.out.println(d);
		
		System.out.println();
		
		System.out.println("Naive value");
		System.out.println(n);
		


	}

 //hello again 
	/*
	 * results in a transformation of input DAG topology. 'blocks' is a list of lists, each inner list containing the nodes that can be executed
	 * given that its predecessors have already been executed.
	 *
	 * */
	public static void arrangeByPrecCons(Node node, ArrayList<ArrayList<Integer>> blocks)
	{

		ArrayList<Integer> block = new ArrayList<Integer>();
		block = getBlock(node);
		while(!block.isEmpty())
		{
			blocks.add(block);
			block = getBlock(node);
		}

		resetDagAccounted(node);

	}

	public static boolean arePredecessorsAccounted(Node node)
	{
		ArrayList<Node> prevs = node.getPrev();
		int count = 0;
		for(int i = 0; i < prevs.size(); i++)
		{
			if(prevs.get(i).accounted == true) {
				count++;
			}
		}

		if (count == prevs.size()) {
			return true;
		}
		else return false;
	}

	public static ArrayList<Integer> getBlock(Node node)
	{
		resetDag(node);
		ArrayList<Integer> block = new ArrayList<Integer>();

		node.visit = true;
		Queue<Node> q = new LinkedList<Node>();
		Queue<Node> accountedFor = new LinkedList<Node>();
		q.add(node);
		while(!q.isEmpty()) {
			Node target = q.remove();
			if(target.accounted == false && arePredecessorsAccounted(target)) {
				//target.accounted = true;
				block.add(target.getId());
				accountedFor.add(target);
			}
			ArrayList<Node> nexts = target.getNext();
			for(int i = 0; i < nexts.size(); i++) {
				Node next = nexts.get(i);
				if(next.visit != true) {
					next.visit = true;
					q.add(next);
				}
			}
		}


		while(!accountedFor.isEmpty())
		{
			Node acct = accountedFor.remove();
			//System.out.print(acct.getId() + " ");
			acct.accounted = true;
		}
		//System.out.println();

		resetDag(node);

		return block;
	}


	//verified this works as expected with testcases
	public static void printBlocks(ArrayList<ArrayList<Integer>> blocks)
	{
		for (int i = 0; i < blocks.size(); i++)
		{
			for (int j = 0; j < blocks.get(i).size(); j++)
			{
				System.out.print(blocks.get(i).get(j) + " ");
			}
			System.out.println();
		}
	}
	
	public static ArrayList<ArrayList<Integer>> buildNaive(ArrayList<ArrayList<Integer>> blocks)
	{
		ArrayList<ArrayList<Integer>> naive = new ArrayList<ArrayList<Integer>>();
		ArrayList<Integer> temp = new ArrayList<Integer>();
		
		for (int i = 0; i < blocks.size(); i++)
		{
			for (int j = 0; j < blocks.get(i).size(); j++)
			{
				temp.add(blocks.get(i).get(j));
			}
		}
		
		for(int k = 0; k < temp.size(); k++)
		{
			ArrayList<Integer> seg = new ArrayList<Integer>();
			seg.add(temp.get(k));
			naive.add(seg);
		}
		
		
		
		return naive;
	}
	
	public static void printNaive(ArrayList<ArrayList<Integer>> naive)
	{
		for (int i = 0; i < naive.size(); i++)
		{
			for (int j = 0; j < naive.get(i).size(); j++)
			{
				System.out.println(naive.get(i).get(j));
			}
			
		}
		System.out.println();
	}

	public static void resetDag(Node node)
	{
		ArrayList<Node> nexts = node.getNext();

		if(node.visit == true)
			node.visit = false;

		for(int i = 0; i < nexts.size(); ++i)
			resetDag(nexts.get(i));
	}

	public static void resetDagAccounted(Node node)
	{
		ArrayList<Node> nexts = node.getNext();

		if(node.accounted == true)
			node.accounted = false;

		for(int i = 0; i < nexts.size(); ++i)
			resetDag(nexts.get(i));
	}

	public static NodeGroup transformToNodeGroup(ArrayList<ArrayList<Integer>> blocks)
	{
		NodeGroup nodeGroup = new NodeGroup();
		NodeGroup nodeGroupStart = nodeGroup;
		
		for (int i = 0; i < blocks.size(); i++)
		{
			for (int j = 0; j < blocks.get(i).size(); j++)
			{
				Node node = new Node(blocks.get(i).get(j));
				nodeGroupStart.addNode(node);
			}
			if(i != blocks.size() - 1)
			{
				NodeGroup nextNodeGroup = new NodeGroup();
				nodeGroupStart.setNext(nextNodeGroup);
				nodeGroupStart = nodeGroupStart.getNext();
			}
			
		}
		
		return nodeGroup;
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

	/*This code takes the first node in graph and adds it to a queue.
	 * While the queue is not empty, it will look at each node in the queue
	 * and determine if that node has multiple previous or multiple next nodes,
	 * if it does, it will then add that node to a stack. It will then get the
	 * list of next nodes from the current node that was in the queue and
	 * add all of those nodes to the queue to be explored. By the end of the
	 * while loop, all nodes should have been examined and all fork and join
	 * nodes will have been added to stack which is returned to calling function.
	 * I employed a BFS algorithm for looking at this graph. Refer to wikipedia if confused*/
	/*
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
	*/
	public static void parseArgs(String args[])
	{
		for(int i=0; i<args.length; i=i+2)
		{
			switch(args[i])
			{
				case "-n":
					num_node = Integer.parseInt(args[i+1]);
					break;
				case "-e":
					num_edge = Integer.parseInt(args[i+1]);
					break;
				case "-g":
					gen_mode = Integer.parseInt(args[i+1]);
					break;
				case "-gr":
					group_mode = Integer.parseInt(args[i+1]);
					break;
				case "-ng":
					ng_print = true;
			}
		}

		if(num_node == 0)
		{
			/**** Change Number of Nodes Here ******/
			num_node = 7;
			//num_node = ThreadLocalRandom.current().nextInt(5, 20);
			System.out.print("Random Generate ");
		}
		if(num_edge == 0)
			num_edge = (num_node-2)*(num_node-1)/6;

	}//}}}
}

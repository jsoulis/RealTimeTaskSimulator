package multicore_exp;

import java.util.ArrayList;

import data.Task;
import data.TaskSet;
import generator.TaskGenerator;
import generator.TaskGenerator2;
import period_harmonizer.PeriodModifier;

public class DAGTest_Extension {

	public DAGTest_Extension(int numVertex)
	{		
		this.numVertex = numVertex;
	}
	
	/*
	 * This merges the task set into one task apparently, so we can look at peak density appropriately
	 * assume this is going to happen for every similar sequential Segment in our General DAG task model 
	 * as well
	*/
	public double getDensity(int seed, ArrayList<ArrayList<Integer>> nodeinfo)
	{
		// COMMENTED OUT THIS LINE BELOW
		//System.out.println(nodeinfo);
		/*********************/
		Param.NumProcessors = 8;
		Param.NumThreads_MAX = 8;
		/*********************/
		Param.scmin = 10;
		Param.scmax = 1000;
		Param.Period_MIN = 200;
		Param.Period_MAX = 1000;
		Param.NumSegments_MIN = numVertex;
		Param.NumSegments_MAX = numVertex;
		
	//	PeriodModifier periodModifier = new PeriodModifier(10);
		TaskGenerator generator = new TaskGenerator();

		generator.setRandomBeta(0.3, 2);
	//generator.setFixedBeta(2.0);
		/***********************************/
		generator.setFixedGamma(1.0);
		/***********************************/
		generator.setRandomAlpha(0, 0.5);/**/
		generator.setFixedTaskNum(1);
		
		
		TaskSet taskSet = generator.GenerateTaskSet(seed, seed);
		TaskSet mergedTaskSet = new TaskSet();

		Task task = taskSet.get(0);
		
		Task mergedTask = new Task(nodeinfo.size(), task.getPeriod(), task.getDeadline());
		
		for (int i = 0; i < nodeinfo.size(); i++) //for each segment
		{
			for (int k = 0; k < task.getNumOptions(); k++) //for each thread
			{
				int maxExecutionTime = 0;
				for (int l = 0; l < k + 1; l++)
				{
					int executionSum = 0;
					for (int j = 0; j < nodeinfo.get(i).size(); j++)
					{
						int executionTime = task.getExecutionTimeOfThread(nodeinfo.get(i).get(j), k, l);
						if (executionTime > maxExecutionTime) maxExecutionTime = executionTime;

						executionSum += executionTime;
						//executionSum += task.getExecutionTimeOfThread(nodeinfo.get(i).get(j), k, l)* (1 + (j / 10.0));
					}
					mergedTask.setExecutionTime(i, k, l, executionSum);
				}
				mergedTask.segments[i].get(k).maxExecutionTime = maxExecutionTime;
			}
		}
		
		mergedTaskSet.add(mergedTask);
		
//		System.out.println(task);
//		System.out.println(mergedTask);
		
		// COMMENTED OUT
		//System.out.println(logic.RTAS.getPeakDensity(mergedTaskSet));		
		//System.out.println(logic.RTAS.getPeakDensity(taskSet));		
		
		
	
		return logic.RTAS.getPeakDensity(mergedTaskSet);		
		//return logic.NBG.getPeakDensity(mergedTaskSet, logic.NBG.Type.SINGLE);
		//return logic.NBG.getPeakDensity(mergedTaskSet, logic.NBG.Type.MAX);
	}
	
	public double getPeakDensity(ArrayList<ArrayList<Double>> densityGraphs)
	{
		
		/*
		 * TO DO:  For each individual task in the task set we will look at the optimal density. 
		 * The peak density this function returns should be the SUM of all the optimal densities calculated for
		 * each task. The optimal density for each task will be calculated using the approximate density graphs.
		 * The approximate density graphs will be used in the following way. We will add up the density graphs, in sequence or 
		 * in parallel. and we will have a final graph that represents the final DAG structure. WE will then plug in the task deadline to 
		 * this final graph and the final result will be the opStimal density for that task! This value is saved and then added to all the other values for the segments. 
		 */
		
		
		return 0.0;
	}
	
	public ArrayList<ArrayList<Double>> createDensityGraphs(Task task, ArrayList<ArrayList<Integer>> blocks)
	{
		ArrayList<ArrayList<Double>> densityGraphs = new ArrayList<ArrayList<Double>>();
		
		return densityGraphs;
		
	}
	
	public ArrayList<Double> sumVertically(ArrayList<Double> graph_one, ArrayList<Double> graph_two)
	{
		ArrayList<Double> sum = new ArrayList<Double>();
		sum.add(0.0);
		sum.add(0.0);
		sum.add(0.0);
		sum.add(0.0);
		
		double X1 = 0.0;
		double Y1 = 0.0;
		double X2 = 0.0;
		double Y2 = 0.0;
		
		//left x and y value of graph
		if(graph_one.get(0) > graph_two.get(0))
		{
			X1 = graph_one.get(0);
			Y1 = graph_one.get(1) + getYValueFromGraph(graph_two, X1);
		}
		else if(graph_one.get(0) < graph_two.get(0))
		{
			X1 = graph_two.get(0);
			Y1 = graph_two.get(1) + getYValueFromGraph(graph_one, X1);
		}
		else 
		{
			X1 = graph_one.get(0);
			Y1 = graph_one.get(1) + graph_two.get(3);
		}
		
		
		//right x and y value of graph 
		if(graph_one.get(0) > graph_two.get(0))
		{
			X1 = graph_two.get(0);
			Y1 = graph_two.get(1) + getYValueFromGraph(graph_one, X1);
		}
		else if(graph_one.get(0) < graph_two.get(0))
		{
			X1 = graph_one.get(0);
			Y1 = graph_one.get(1) + getYValueFromGraph(graph_two, X1);
		}
		else 
		{
			X1 = graph_one.get(0);
			Y1 = graph_one.get(1) + graph_two.get(3);
		}
		
		
		sum.set(0, X1);
		sum.set(1, Y1);
		sum.set(2, X2);
		sum.set(3, Y2);
		
		return sum;
	}
	
	public ArrayList<Double> sumHorizontally(ArrayList<Double> graph_one, ArrayList<Double> graph_two)
	{
		ArrayList<Double> sum = new ArrayList<Double>();
		ArrayList<Double> inverseSum = new ArrayList<Double>();
		ArrayList<Double> inverseGraph_one = new ArrayList<Double>();
		ArrayList<Double> inverseGraph_two = new ArrayList<Double>();
		
		inverseGraph_one = getInverseGraph(graph_one);
		inverseGraph_two = getInverseGraph(graph_two);
		
		inverseSum = sumVertically(inverseGraph_one, inverseGraph_two);
		
		sum = getInverseGraph(inverseSum);
		
		return sum;
	}
	
	public double getYValueFromGraph(ArrayList<Double> graph, double deadline)
	{
		double result = 0.0;
		double slope = 0.0;
		slope = ((graph.get(3) - graph.get(1)) / (graph.get(2) - graph.get(0)));
		result = slope*deadline - slope*graph.get(0) + graph.get(1);
		return result;
	}
	
	public ArrayList<Double> getInverseGraph(ArrayList<Double> graph)
	{
		ArrayList<Double> inverseGraph = new ArrayList<Double>();
		inverseGraph.add(graph.get(1));
		inverseGraph.add(graph.get(0));
		inverseGraph.add(graph.get(3));
		inverseGraph.add(graph.get(2));
		
		return inverseGraph;
	}
	
	private int numVertex;
	


}
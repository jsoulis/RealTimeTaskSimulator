package logic;


import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import multicore_exp.Param;
import multicore_exp.TaskSetPool;
import data.Task;
import data.TaskSet;






public class RTAS {

	/**
	 * DeadlinDensitySnippet class contains information about
	 * the minimum possible deadline(d) and its corresponding density(delta)
	 * delta is defined as total_execution_time / deadline 
	 */
	public class DeadlineDensitySnippet
	{		
		/**Just need a maxThreadExecutionTime and a totalExecutionTime in order to define a
		 * DeadlineDensitySnippet. We therefore can use this data structure to define the additions
		 * of different snippets that is done when adding sideways (nodes in the same sequence)
		 * and vertically (sequences before joining to a node). Keep in mind vertical addition
		 * is done such that the deadline is the same and the density is summed for a given
		 * deadline. **/
		
		private int nOptions; // parallelization option
		private double totalExecutionTime; //total execution time, added exeTimes of all threads together
		private double maxThreadExecutionTime; //largest exeTime amoung all the threads

		/**
		 * @param nOptions the number of parallelization options
		 * @param maxThreadExecutionTime maximum execution time among nOptions threads
		 * @param totalExecutionTime the sum of the execution time of all threads
		 */
		public DeadlineDensitySnippet(int nOptions, double maxThreadExecutionTime, double totalExecutionTime)
		{
			this.nOptions = nOptions;
			this.maxThreadExecutionTime = maxThreadExecutionTime;	// C^max_ik A.K.A minimum deadline
			this.totalExecutionTime = totalExecutionTime;			
		}
		
		/**
		 * @return possible maximum density of snippet defined as (total execution time / maximum thread execution time) 
		 * deadline is smallest when it equals the maxThreadExecutionTime
		 */
		public double getMaxDensity()
		{
			return totalExecutionTime / maxThreadExecutionTime;
		}
		
		/**
		 * @param deadline objective deadline
		 * @return the density of given deadline
		 */
		
		public double getDenstiy(double deadline)
		{
			return totalExecutionTime / deadline;
		}
		
		/**
		 * @param density objective deadline
		 * @return deadline of given density
		 */
		public double getDeadline(double density)
		{			
			if (density > getMaxDensity()) return totalExecutionTime / getMaxDensity();
			return totalExecutionTime / density;
		}
		
		/**
		 * @return the number of options of this snippet
		 */
		public int getOption()
		{
			return nOptions;
		}
		
		/**
		 * @return total execution time
		 */
		public double getTotalExecutionTime()
		{
			return totalExecutionTime;
		}
	}

/**This is one of the density/deadline graphs for a segment**/
	public class SegmentSnippets
	{
		private ArrayList<DeadlineDensitySnippet> snippets;
		public SegmentSnippets()
		{
			snippets = new ArrayList<DeadlineDensitySnippet>();
		}
		
		public void addSnippet(DeadlineDensitySnippet snippet)
		{
			snippets.add(snippet);			
		}
		
		/**
		 * @param density given density
		 * @return optimal deadline density snippet of the segment for given density
		 */
		public DeadlineDensitySnippet getOptimalSnippet(double density)
		{
			double minDeadline = -1;
			int snippetIndex = 0;
			for (int i = 0; i < snippets.size(); i++)
			{
				if (minDeadline < 0 || minDeadline > snippets.get(i).getDeadline(density))
				{
					minDeadline = snippets.get(i).getDeadline(density);
					snippetIndex = i;
				}				
			}
			
			return snippets.get(snippetIndex);
		}
	}

	public class TaskSnippets
	{
		private ArrayList<SegmentSnippets> taskSnippets = new ArrayList<SegmentSnippets>();
		
		/**Sorted snippets is a combination of all the segment snippets, ordered by greatest density first**/
		private ArrayList<DeadlineDensitySnippet> sortedSnippets = new ArrayList<DeadlineDensitySnippet>();
		private ArrayList<DeadlineDensitySnippet> optimalSnippets;
		private double deadline;
		private double optimalDensity;

		//constructor
		public TaskSnippets(Task task)
		{			
			this.deadline = task.getDeadline();
			for (int i = 0; i < task.getNumSegments(); i++)
			{
				SegmentSnippets segmentSnippets = new SegmentSnippets();
				for (int j = 0; j < task.getNumOptions(i); j++)
				{
					int nOptions = j + 1;
					double totalExecutionTime = task.getTotalExecutionTime(i, j);				
					double maxThreadExecutionTime = task.getMaxExecutionTimeOfSegment(i, j);
					
					DeadlineDensitySnippet snippet = new DeadlineDensitySnippet(nOptions, maxThreadExecutionTime, totalExecutionTime);
					segmentSnippets.addSnippet(snippet);
					sortedSnippets.add(snippet);
				}
				taskSnippets.add(segmentSnippets);				
			}
			Collections.sort(sortedSnippets, Collections.reverseOrder(new Comparator<DeadlineDensitySnippet>() {
		        @Override
		        public int compare(DeadlineDensitySnippet  snippet1, DeadlineDensitySnippet  snippet2)
		        {
		        	return Double.compare(snippet1.getMaxDensity(), snippet2.getMaxDensity());
		        }
		    }));
			
			findOptimalDensity();
		}
		
		/**Adds the deadlines together of the density/deadline segment graphs...whose points were sorted
		 * in sorted snippets**/
		private double getTotalDeadline(double density)
		{
			double totalDeadline = 0;
			for (int i = 0; i < taskSnippets.size(); i++)
			{
				totalDeadline += taskSnippets.get(i).getOptimalSnippet(density).getDeadline(density);
			}
			
			return totalDeadline;
		}
		
		/**
		 * @param density
		 * @return a set of deadline-density-snippets. It contains each segment's proper snippet according to the given density.
		 */
		private ArrayList<DeadlineDensitySnippet> getSnippetsByDensity(double density)
		{
			ArrayList<DeadlineDensitySnippet> snippets = new ArrayList<DeadlineDensitySnippet>();
			for (int j = 0; j < taskSnippets.size(); j++)
			{
				snippets.add(taskSnippets.get(j).getOptimalSnippet(density));
			}
			return snippets;
		}		
		
		private ArrayList<DeadlineDensitySnippet> getSnippetsByDeadline(double deadline)
		{			
			double feasibleDensity = -1;
			for (int i = 0; i < sortedSnippets.size(); i++)
			{
				// higher density leads shorter deadline. 
				// thus, deadline monotonically increases in this loop
				// if the density that makes larger deadline than given is found,
				// then very previous step is the feasible one. 
				
				double currentDensity = sortedSnippets.get(i).getMaxDensity();				
				if (getTotalDeadline(currentDensity) > deadline)
				{
					// if the first set is not feasible than there is no solution
					if (feasibleDensity < 0) 
						return null;

					// previous set is feasible one
					return getSnippetsByDensity(feasibleDensity);
				}
				feasibleDensity = currentDensity;
			}
			
			// all set is feasible. the last one has the smallest density
			return getSnippetsByDensity(feasibleDensity);	
		}
		
		public void findOptimalDensity()
		{
			optimalDensity = 0;
			
			/**We look at sorted snippets. Which is an order on the segment snippets. Each segment snippet
			 * has a value of density, and snippets are ordered based on how high there density is, so 
			 * the sorted snippets is an order of the entire pool of densitySnippets that exist in the
			 * segmentSnipets. The function below, the finds the optimal snippets. These snippets 
			 * are the snippets in which the optimal density values exist, this is determining by looking
			 * at each snippet from largest density downward and then looking at that density on each
			 * segmentSnippet. The deadlines can than be obtained for each segmentSnippet and then
			 * added together. We do this until the sum exceeds the total task deadline. This function
			 * then returns the densitySnippet from each segmentSnippet from the previous search in which
			 * the total sum of deadlines did not exceed task deadlines. The optimal density will be found
			 * within these segments, which we call the optimal segments **/
			optimalSnippets = getSnippetsByDeadline(deadline);			
			
			if (optimalSnippets == null)
			{
				optimalDensity = -1;
//				System.out.println("Optimal Density = not feasible");
				return;
			}
			
			
			for (int i = 0; i < optimalSnippets.size(); i++)
			{
				optimalDensity += optimalSnippets.get(i).getTotalExecutionTime();
			}
			optimalDensity /= deadline;

/*			
			System.out.println("Optimal Density = " + optimalDensity);
			for (int i = 0; i < optimalSnippets.size(); i++)
				System.out.println("Segment " + (i + 1) + " : option=" + getOptimalOption(i) + " deadline=" + getIntermediateDeadline(i));
*/				
						
		}
		
		public double getOptimalDensity()
		{
			if (optimalDensity < 0) return Double.POSITIVE_INFINITY;
			if (optimalDensity < 0) return Param.NumProcessors * 2;
			return optimalDensity;
		}
		
		public int getOptimalOption(int segmentIndex)
		{
			return optimalSnippets.get(segmentIndex).getOption();
		}
		
		public double getIntermediateDeadline(int segmentIndex)
		{
			return optimalSnippets.get(segmentIndex).getDeadline(optimalDensity);
		}
	}	
	
	
	public static boolean isSchedulable(TaskSet taskSet)
	{
		if (getNumSchedulableTasks(taskSet) == taskSet.size()) 
			return true;
		return false;	 
	}
	
	public static boolean isSchedulable(TaskSetPool taskPool)
	{
		if (getNumSchedulableTaskSets(taskPool) == taskPool.size()) 
			return true;
		return false;
	}
	
	public static int getNumSchedulableTasks(TaskSet taskSet) {
		
		RTAS rtas = new RTAS();
		
		
		ArrayList<TaskSnippets> taskSnippets = new ArrayList<TaskSnippets>();
		
		double worstCaseTotalDensity = 0;
		
		for (int i = 0; i < taskSet.size(); i++)
		{
//			System.out.println("-----------------------Task " + (i + 1) + "-----------------------------" + taskSet.getTaskSetID());
			
			TaskSnippets taskSnippet = rtas.new TaskSnippets(taskSet.get(i));
			worstCaseTotalDensity += taskSnippet.getOptimalDensity();
			
			taskSnippets.add(taskSnippet);
			if (worstCaseTotalDensity > Param.NumProcessors) return i;
		}
		
		return taskSet.size();		
	}
	
	public static int getNumSchedulableTaskSets(TaskSetPool taskPool)
	{
		int success = 0;
		for (int i = 0; i < taskPool.size(); i++)
		{
			TaskSet taskSet = taskPool.get(i);
			
			if (isSchedulable(taskSet))
				success ++;				
		}
		
		return success;
	}
	
	public static double getPeakDensity(TaskSet taskSet)
	{
		RTAS rtas = new RTAS();
		
		double worstCaseTotalDensity = 0;
		
		for (int i = 0; i < taskSet.size(); i++)
		{			
			TaskSnippets taskSnippet = rtas.new TaskSnippets(taskSet.get(i));
			worstCaseTotalDensity += taskSnippet.getOptimalDensity();			
		}
		
		return worstCaseTotalDensity;		
	}
	
	public static int Check(TaskSetPool taskPool)
	{
		return getNumSchedulableTaskSets(taskPool);
	}
}

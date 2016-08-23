#ifndef __EXPERIMENT__
#define __EXPERIMENT__

#include <iostream>
#include <fstream>

#include "TaskSet.h"
#include "rta.h"
#include "bar.h"

class Experiment
{
	private:
		int repeat;
		//Generator gen;
		TaskSet ts;

	public:
		Experiment();
		int run();
		int output();
};

#endif

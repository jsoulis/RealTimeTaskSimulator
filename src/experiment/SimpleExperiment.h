#ifndef __SIMPLE_EXP__
#define __SIMPLE_EXP__

#include "../experiment/Experiment.h"
#include "../generator/SimpleGenerator.h"
#include "../test/GFB.h"

class SimpleExperiment : public Experiment
{
private:
	int init();
	SimpleGenerator sg;
	GFB gfb;
	bool schedulable;
public:
	SimpleExperiment();
	int set();
	int run();
	int output();
};

#endif
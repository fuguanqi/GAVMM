import java.util.Random;

public class Main {
	static double ALPHA = 0.999;
	static double BETA = 0.001;
	static int nVMs = 30;     //number of VMs
	static int nServers = 10;  //Number of Serers
	static int popSize = 20; //20-100, 20 per step
    static double crossoverRate = 0.6;
    static double mutationRate = 0.1;
    static int nIterations = 5000;  //最大迭代次数
    static int nStageCount = 50;    //连续nStageCount代最适应个体无变化则结束循环
	static double overloadRate = 0.4; //过载的虚机占全部虚机的比例
	static int nOverloadVMs= (int)Math.round(nVMs * overloadRate); //过载虚拟机个数
	static int capacityUp =500;  //主机容量（均匀分布）上限
	static int capacityLow = 30;  //主机容量（均匀分布）下限
	static int loadUp = 20;     //虚机负载（均匀分布）上限
	static int loadLow = 0;     //虚机负载（均匀分布）下限
	static int overloadLow=(int)Math.round(loadUp- (loadUp- loadLow)*overloadRate);//定义过载额
	static int distanceUp = 100;     //主机间距离（均匀分布）上限
	static int distanceLow = 0;     //主机间距离（均匀分布）下限
	static int weightUp = 1000;     //虚机间依赖weight（均匀分布）上限
	static int weightLow = -20;     //虚机间依赖weight（均匀分布）下限,负值将作为0处理
	

	/**
	 * 随机数
	 */
	static Random random;

    /**
	 *  主机容量
	 */
	static int[] serverCapacity;

	/**
	 *  虚机负载
	 */
	static int[] vMLoad;  

	/**
	 *  主机间距离矩阵
	 */
	static int[][] distances;

	/**
	 *  虚机间通信矩阵
	 */
	static int[][] dependencies;

	/**
	 *  初始种群，父代种群，行数表示种群规模，一行代表一个个体，即染色体，列表示染色体基因片段
	 */
	static int[][] oldPopulation;
	
	/**
	 * 新的种群，子代种群
	 */
	static int[][] newPopulation;
	

	/**
	 * 初始状态时的虚机映射
	 */
	static int[] originalMapping;


	/**
	 * 储存最优解
	 */
	static int[] bestChromosome;


	/**
	 * 种群适应度，表示种群中各个个体的适应度
	 */
	static double[] fitness;

	/**
	 * 存储最佳适应度
	 */
	static double bestFitness;


	/**
	 * 种群中个体的生存适应度总和
	 */
	static double popFitness;

	
    /**
	 * 生成一个0-65535之间的随机数
	 * @return
	 */
	static int getRandomNum(){
		return random.nextInt(65535);
	}


	/**
	 * 检验染色体是否为满足约束条件的可行解
	 */
	static boolean validate(int[] chromosome){
		if(chromosome.length!=nOverloadVMs){
			return false;
		}
		int[] capacityDemand = new int[nServers];
		int[] mapping = new int[nVMs];
		System.arraycopy(chromosome,0,mapping,0,nOverloadVMs);
		System.arraycopy(originalMapping,nOverloadVMs,mapping,nOverloadVMs,nVMs- nOverloadVMs);
		for (int i=0;i<mapping.length;i++){
			capacityDemand[mapping[i]]+=vMLoad[i];
		}
		for(int i=0;i<nServers;i++){
			if(capacityDemand[i]>serverCapacity[i]){
				return true;
			}
		}
		return true;
	}


	/**
	 * 随机生成可行解
	 */
	static int[] generateChromosome(){
		int[] newChromosome = new int[nOverloadVMs];
		for(int i=0;i<nOverloadVMs;i++){
			newChromosome[i]=getRandomNum()%nServers;
			// System.out.println("****"+newChromosome[i]+"***");
		}

		if(validate(newChromosome)){
			return newChromosome;
		}
		else {
			return generateChromosome();
		}
	}

	/**
	 * 生成可行初始映射
	 */
	static int[] generateOriginalMapping(){
		int[] newMapping = new int[nVMs];
		for(int i=0;i<nVMs;i++){
			newMapping[i]=getRandomNum()%nServers;
		}
		int[] capacityDemand = new int[nServers];
		for (int i=0;i<newMapping.length;i++){
			capacityDemand[newMapping[i]]+=vMLoad[i];
		}
		for(int i=0;i<nServers;i++){
			if(capacityDemand[i]>serverCapacity[i]){
				return generateOriginalMapping();
			}
		}
		return newMapping;
	}



	/**
	 * 初始化各类输入变量，也可改写另人工赋值
	 */
	static void initialize(){
		random = new Random(20);//初始化随机数

		//初始化主机容量
		serverCapacity = new int[nServers];
		for(int i=0;i<nServers;i++){
			serverCapacity[i]=getRandomNum()%(capacityUp - capacityLow)+capacityLow;
		}
		System.out.println("\n nServers="+nServers+", serverCapacity:");
		for(int i=0;i<nServers;i++){
			System.out.println("Server "+i+": "+serverCapacity[i]);
		}

		//初始化虚机负载
		vMLoad = new int[nVMs];
		for(int i=0;i<nVMs;i++){
			if(i<nOverloadVMs){           //方便起见，过载虚机的编号都放在最前
				vMLoad[i]=getRandomNum()%(loadUp - overloadLow)+overloadLow;
			}
			else {
				vMLoad[i]=getRandomNum()%(overloadLow - loadLow)+loadLow;
			}
		}
		System.out.println("\n nVMs="+nVMs+", vMLoad:");
		for(int i=0;i<nVMs;i++){
			System.out.println("VM "+i+": "+vMLoad[i]);
		}

		//初始化主机间距离矩阵
		distances = new int[nServers][nServers];
		for(int i=0;i<nServers;i++){
			for(int j=0;j<nServers;j++){
				if(i!=j){
					distances[i][j]=getRandomNum()%(distanceUp - distanceLow)+capacityLow;
				}
				else{
					distances[i][j]=0;
				}
				
			}
		}
		System.out.println("\n distances:");
		for(int i=0;i<nServers;i++){
			for(int j=0;j<nServers;j++){
				System.out.print(distances[i][j]+"  ");
			}
			System.out.println();
		}

		//初始化虚机间通信矩阵
		dependencies = new int[nVMs][nVMs];
		for(int i=0;i<nVMs;i++){
			for(int j=0;j<nVMs;j++){
				if(i!=j){
					dependencies[i][j]=Math.max(getRandomNum()%(weightUp - weightLow)+weightLow,0);
				}
				else{
					dependencies[i][j]=0;
				}
				
			}
		}
		System.out.println("\n dependencies:");
		for(int i=0;i<nVMs;i++){
			for(int j=0;j<nVMs;j++){
				System.out.print(dependencies[i][j]+"  ");
			}
			System.out.println();
		}

		//初始状态时的映射
		originalMapping= generateOriginalMapping();
		System.out.println("\n originalMapping: \n");
		for(int i=0;i<nVMs;i++){
			System.out.println(originalMapping[i]);
		}

		//初始化old种群
		oldPopulation= new int[popSize][nOverloadVMs];
		System.arraycopy(originalMapping,0,oldPopulation[0],0,nOverloadVMs);
		for(int i=1;i<popSize;i++){
			System.arraycopy(generateChromosome(),0,oldPopulation[i],0,nOverloadVMs);
			// System.out.println("\n oldPopulation "+i+": ");
			// for(int j=0;j<nOverloadVMs;j++){
			// 	System.out.print(oldPopulation[i][j]);
			// }
		}

		//初始化new种群
		newPopulation= new int[popSize][nOverloadVMs];
		
		//初始化适应度向量
		fitness= new double[popSize];
		popFitness=0.0;
		for(int i=0;i<popSize;i++){
        	fitness[i]=calFitness(oldPopulation[i]);
       		popFitness+=fitness[i];
       	}	

       	bestChromosome=new int[nOverloadVMs];
       	System.arraycopy(originalMapping,0,bestChromosome,0,nOverloadVMs); 
		bestFitness=calFitness(bestChromosome);
	}

	/**
	 * 评价函数，用于计算适度
	 * @param index 当前染色体的下标
	 * @param chromosome 染色体
	 * @return the fitness of a specific chromosome;
	 */
	static double calFitness(int[] chromosome){
		int[] mapping=new int[nVMs];
		System.arraycopy(chromosome,0,mapping,0,nOverloadVMs);
		System.arraycopy(originalMapping,nOverloadVMs,mapping,nOverloadVMs,nVMs - nOverloadVMs);

		//commnication cost
		int commCost=0;
		double commCostStd=0.0;
		int eTopoVm=0;
		int maxW=0;
		int maxD=0;
		for(int i=0;i< nVMs;i++){
			int m= mapping[i];
			for(int j=0;j<nVMs;j++){
				int n =mapping[j];
				commCost+=dependencies[i][j]*distances[m][n];
				if(dependencies[i][j]>0){
					eTopoVm++;
				}
				if(dependencies[i][j]>maxW){
					maxW=dependencies[i][j];
				}
				if(distances[m][n]>maxD){
					maxD=distances[m][n];
				}
			}
		}
		commCostStd=commCost*1.0/(eTopoVm * maxW * maxD);  //标准化

		int migCost=0;
		double migCostStd=0.0;
		for(int i=0;i<nOverloadVMs;i++){
			int s = originalMapping[i];
			int d = mapping[i];
			migCost+=vMLoad[i]* distances[s][d];  //假设视load与size一致
		}
		migCostStd=migCost*1.0/(nOverloadVMs*loadUp*distanceUp);  //标准化


        // System.out.println(commCostStd+"$$$$$$"+migCostStd);
		return 1-(ALPHA*commCostStd+BETA*migCostStd); //成本越低适应度越高

	}



	/**
	 * 选择算子，赌轮选择策略挑选1个下一代个体
	 */
	static int rouletteWheelSelect(){
		double tmpRan;
		double tmpSum;
		tmpRan = (double)((getRandomNum() % 1000) / 1000.0);
		tmpSum = 0.0;
		for (int j = 0; j < popSize; j++) {
			tmpSum += fitness[j]/popFitness;
			if (tmpSum > tmpRan) {
				return j;
			}
		}
		return -1;
	}

	static int[][] crossOver(int[] a, int[] b){
		double tmpRan;
		int[][] result = new int[2][nOverloadVMs];
		tmpRan = (double)((getRandomNum() % 1000) / 1000.0);
		if(tmpRan<crossoverRate){
			int crossOverIndex = getRandomNum() % nOverloadVMs;
			int [] tmpChr = new int[nOverloadVMs];
			System.arraycopy(a,0,tmpChr,0,nOverloadVMs);
			System.arraycopy(b,crossOverIndex,a,crossOverIndex,nOverloadVMs- crossOverIndex);
			System.arraycopy(tmpChr,crossOverIndex,b,crossOverIndex,nOverloadVMs- crossOverIndex);
		}
		System.arraycopy(a,0,result[0],0,nOverloadVMs);
		System.arraycopy(b,0,result[1],0,nOverloadVMs);
		if(validate(result[0]) && validate(result[1])){
			return result;
		}
		else{
			return crossOver(a, b);
		}
	}

	static int[] mutation(int[] chromosome){
		double tmpRan= (double)((getRandomNum() % 1000) / 1000.0);
		int[] result = new int[nOverloadVMs];
		System.arraycopy(chromosome,0,result,0,nOverloadVMs);
		if(tmpRan<mutationRate){
			int mutationIndex = getRandomNum() % nOverloadVMs;
			int alterServer = getRandomNum() % nServers;
			result[mutationIndex]=alterServer;
			if(validate(result)){
				return result;
			}
			else return mutation(chromosome);
		}
		return result;

	}


    public static void main(String []args) {
        System.out.println("GAVMM：\n");
        initialize();
        int iter=0;
        int stageCount=0;
        while(iter<nIterations && stageCount<nStageCount){
       		System.out.println("iter:"+iter);
			int newSize=0;

			while(newSize<popSize){
				int a = rouletteWheelSelect();
				int b = rouletteWheelSelect();
				int[][] children = crossOver(oldPopulation[a],oldPopulation[b]);
				int [] c = mutation(children[0]);
				int [] d = mutation(children[1]);
				System.arraycopy(c,0,newPopulation[newSize],0,nOverloadVMs);
				newSize++;
				if(newSize<popSize){
					System.arraycopy(d,0,newPopulation[newSize],0,nOverloadVMs);
					newSize++; 
				}
 			}
 			oldPopulation=newPopulation;
			newPopulation= new int[popSize][nOverloadVMs];
 			popFitness=0.0;
 			double iterBestFitness=0.0;
 			int iterBestIndex=0;
 			for(int i=0;i<popSize;i++){
        		fitness[i]=calFitness(oldPopulation[i]);
        		popFitness+=fitness[i];
        		if(iterBestFitness<fitness[i]){
        			iterBestFitness=fitness[i];
        // 			System.out.println("******"+iterBestFitness+"******");
        			iterBestIndex=i;
        		}
        	}
        	System.out.println("iterBestFitness："+iterBestFitness+"  BestFitness："+bestFitness);
        	if (iterBestFitness>bestFitness){
        		bestFitness=iterBestFitness;
        		System.arraycopy(oldPopulation[iterBestIndex],0,bestChromosome,0,nOverloadVMs);
        		stageCount=0;
        	}
        	else{
        		stageCount++;
        	}
        	iter++;
        }

        System.out.println("The Best chromosome is:\n");
        for(int i=0;i<nOverloadVMs;i++){
        	System.out.println(bestChromosome[i]);
        }
        System.out.println("The Best fitness is:\n"+bestFitness);
    }
}
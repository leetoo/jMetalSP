package org.uma.jmetalsp.examples.dynamictsp;

import org.uma.jmetal.operator.CrossoverOperator;
import org.uma.jmetal.operator.MutationOperator;
import org.uma.jmetal.operator.SelectionOperator;
import org.uma.jmetal.operator.impl.crossover.PMXCrossover;
import org.uma.jmetal.operator.impl.mutation.PermutationSwapMutation;
import org.uma.jmetal.operator.impl.selection.BinaryTournamentSelection;
import org.uma.jmetal.solution.DoubleSolution;
import org.uma.jmetal.solution.PermutationSolution;
import org.uma.jmetal.util.comparator.RankingAndCrowdingDistanceComparator;
import org.uma.jmetalsp.AlgorithmDataConsumer;
import org.uma.jmetalsp.DynamicAlgorithm;
import org.uma.jmetalsp.StreamingDataSource;
import org.uma.jmetalsp.algorithm.mocell.DynamicMOCellBuilder;
import org.uma.jmetalsp.algorithm.nsgaii.DynamicNSGAIIBuilder;
import org.uma.jmetalsp.JMetalSPApplication;
import org.uma.jmetalsp.consumer.LocalDirectoryOutputConsumer;
import org.uma.jmetalsp.DynamicProblem;
import org.uma.jmetalsp.consumer.SimpleSolutionListConsumer;
import org.uma.jmetalsp.observeddata.AlgorithmObservedData;
import org.uma.jmetalsp.problem.tsp.MultiobjectiveTSPBuilderFromFiles;
import org.uma.jmetalsp.impl.DefaultRuntime;
import org.uma.jmetalsp.observeddata.MatrixObservedData;
import org.uma.jmetalsp.observer.Observable;
import org.uma.jmetalsp.observer.impl.DefaultObservable;

import java.io.IOException;
import java.util.List;

/**
 * Example of SparkSP application.
 * Features:
 * - Algorithm: to choose among NSGA-II and MOCell
 * - Problem: Bi-objective TSP
 * - Default streaming runtime (Spark is not used)
 *
 * @author Antonio J. Nebro <antonio@lcc.uma.es>
 */
public class DynamicTSPApplication {

  public static void main(String[] args) throws IOException, InterruptedException {
    JMetalSPApplication<
            MatrixObservedData<Double>,
            AlgorithmObservedData,
            DynamicProblem<DoubleSolution, MatrixObservedData<Double>>,
            DynamicAlgorithm<List<PermutationSolution<Integer>>,AlgorithmObservedData, Observable<AlgorithmObservedData>>,
            StreamingTSPSource,
            AlgorithmDataConsumer<AlgorithmObservedData, DynamicAlgorithm<List<PermutationSolution<Integer>>,AlgorithmObservedData, Observable<AlgorithmObservedData>>>> application;
    application = new JMetalSPApplication<>();

    // Set the streaming data source
    Observable<MatrixObservedData<Double>> streamingTSPDataObservable =
            new DefaultObservable<>("streamingTSPObservable") ;
    StreamingDataSource<?, ?> streamingDataSource = new StreamingTSPSource(streamingTSPDataObservable, 5000) ;

	  // Problem configuration
    DynamicProblem<PermutationSolution<Integer>, MatrixObservedData<Double>> problem ;
    problem = new MultiobjectiveTSPBuilderFromFiles("kroA100.tsp", "kroB100.tsp")
            .build(streamingTSPDataObservable) ;

	  // Algorithm configuration
    CrossoverOperator<PermutationSolution<Integer>> crossover;
    MutationOperator<PermutationSolution<Integer>> mutation;
    SelectionOperator<List<PermutationSolution<Integer>>, PermutationSolution<Integer>> selection;
    crossover = new PMXCrossover(0.9) ;
    double mutationProbability = 0.2 ;
    mutation = new PermutationSwapMutation<Integer>(mutationProbability) ;
    selection = new BinaryTournamentSelection<>(
            new RankingAndCrowdingDistanceComparator<PermutationSolution<Integer>>());

    String defaultAlgorithm = "NSGAII";

    DynamicAlgorithm<List<PermutationSolution<Integer>>, AlgorithmObservedData, Observable<AlgorithmObservedData>> algorithm;
    Observable<AlgorithmObservedData> algorithmObservable = new DefaultObservable<>("") ;

    switch (defaultAlgorithm) {
      case "NSGAII":
        algorithm = new DynamicNSGAIIBuilder<>(crossover, mutation, algorithmObservable)
                .setSelectionOperator(selection)
                .setMaxEvaluations(100000)
                .setPopulationSize(100)
                .build(problem);
        break;

      case "MOCell":
        algorithm = new DynamicMOCellBuilder<>(crossover, mutation, algorithmObservable)
                .setMaxEvaluations(100000)
                .setPopulationSize(100)
                .build(problem);
        break;
      default:
        algorithm = null;
    }

    application.setStreamingRuntime(new DefaultRuntime<MatrixObservedData<Double>, Observable<MatrixObservedData<Double>>, StreamingTSPSource>())
            .setProblem(problem)
            .setAlgorithm(algorithm)
            .addStreamingDataSource(streamingDataSource)
            .addAlgorithmDataConsumer(new SimpleSolutionListConsumer(algorithm))
            .addAlgorithmDataConsumer(new LocalDirectoryOutputConsumer("outputDirectory", algorithm))
            .run();
  }
}

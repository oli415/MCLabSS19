package com.example.activitymonitoring;

import android.os.Handler;
import android.util.Log;

import java.util.Arrays;
import java.util.Random;

/**
 * determines the absolute position, by comparing the reltive movement with the constraints of the map
 *
 * for an detailed explanation see http://research.microsoft.com/pubs/166309/com273-chintalapudi.pdf
 * in our implementation we have made a lot of simplifications:
 *   - direction
 *     - compass reading does not reflect real walking direction as
 *        - influences by objects of the building
 *        - user holding the phone,...
 *      - we don't detect that but use hardcoded values
 *    - movement
 *      - we don't detect steps, nor do we calibrate the step length
 *      - hardcoded velocivty either moving or standing still
 *      - better model would be:
 *        - detect steps... distinguish walk from sudden movement
 *
 */
public class ParticleFilter {
//   float stepFrequency = 1.0f; //in s
   float stepLength = 1.0f; //m feasible stride length 0.5 to 1.2 m  /   up to 10% variation during walk for user

   int numberOfParticles = 10000; //TODO check that in paper

   Floor floor;

   MotionEstimation movement;


    private Handler step_event_handler;


   public Particle[] getParticles() {
      return particles;
   }

   Particle particles[];

   public Position currentPosition;

   ParticleFilter() {
      this.floor = new Floor();
      this.particles = new Particle[numberOfParticles];
      currentPosition = new Position();

      initializeParticles();
   }

   /**
    * draws a random double in the range between min and max
    * @param r initialized Random()
    * @param min lower bound of the range
    * @param max upper bound of the range
    * @return the random value in the range min-max
    */
   private double drawRandomInRange(Random r, double min, double max) {
      return min + r.nextDouble() *(max- min);
   }

   /**
    * ramdomly distributes paritcles at the whole floor
    * TODO could be improved by using wlan rssi values from past localisations, to get a narrow starting distribution
    * using existing localization schemes such as HORUS or EZ to initialize the probability distribution in a more localized area
    */
   public void initializeParticles() {
      double overallArea = floor.getOverallRoomArea();
      double defaultWeight = 1.0d / (double)numberOfParticles;
      int initializedParticles = 0;
      float cummulativeRoomParticleCount = 0; //using cummulative count to avoid rounding errors and therefore difference bigger than 0.5
      Random r = new Random();

      //particles = new Particle[numberOfParticles]; done in constructor

      for(Room room : floor.rooms) {
         cummulativeRoomParticleCount += room.getArea() * numberOfParticles / overallArea;
         //float roomParticleCount = Math.round(room.getArea() * numberOfParticles / overallArea);
         while(initializedParticles < Math.round(cummulativeRoomParticleCount)) {
            double posX  = drawRandomInRange(r, room.getBottomLeftCorner().getX(), room.getTopRightCorner().getX());
            double posY  = drawRandomInRange(r, room.getBottomLeftCorner().getY(), room.getTopRightCorner().getY());
            Position p = new Position(posX, posY);

            particles[initializedParticles] = new Particle(p, p, defaultWeight);
            initializedParticles++;
         }
      }
      Log.i("particleFiler", String.format("initialized %d", initializedParticles));
   }

   //TODO move to appropriate class?
   /**
    * add +/- 10% random noise to value
    * @param r initialized Random
    * @param x the value that gets overloaded with +/- 10% noise
    * @return the value with noise
    */
   public double overloadWithRandomError(Random r, double x) {
       float error = 0.1f;
       x = x + (r.nextDouble() - 0.5) * error * 2 * x;
       return  x;
   }

   /**
    * moves all particles one step in the given direction
    * @param direction
    */
   //TODO add some randomness
   public void moveParticles(double direction) {
       Random r = new Random();
       double direction_rad = Math.toRadians(direction);
       double x_relative = stepLength * Math.sin(direction_rad);
       double y_relative = stepLength * Math.cos(direction_rad);

       x_relative = overloadWithRandomError(r, x_relative);
       y_relative = overloadWithRandomError(r, y_relative);

       for( Particle particle : particles) {
         particle.moveRelative(x_relative, y_relative);
      }
   }


   /**
    * particles, that moved through walls are removed
    * (they will be substituted later by a randomly chosen particle from the last iteration in another step)
    */
   public void removeInvalidMoves() {

      for(Particle particle : particles) {
         Line move = new Line(particle.getLastPosition(), particle.getCurrentPosition());

         for(Line wall : floor.walls) {
            if(move.intersects(wall)) {
                particle.setWeight(0.0d); //at first set to zero later resample
               break;
            }
         }
      }


   }

    public void normalizeWeights() {
        double totalWeight = 0.0;

        // compute total weight
        for (Particle particle : particles) {
            totalWeight += particle.getWeight();
        }

        // normalize particle weights
        for (Particle particle : particles) {
            particle.setWeight(particle.getWeight() / totalWeight);
        }
    }

    public void resampleParticles() {
        Particle[] resampledParticles = new Particle[numberOfParticles];

        // compute particle cdf
        double[] cdf = new double[numberOfParticles];
        cdf[0] = 0.0;
        for (int i = 1; i < numberOfParticles; i++) {
            cdf[i] = cdf[i - 1] + particles[i].getWeight();
        }

        Random rng = new Random();
        double p_step = 1.0 / numberOfParticles; // probability step size for resampling (new sample weight)
        double p_resample = (rng.nextDouble() - 1) * p_step;
        int cdf_idx = 0;

        for (int i = 0; i < numberOfParticles; i++) {
            p_resample += p_step;

            while (cdf_idx < (numberOfParticles - 1) && (p_resample > cdf[cdf_idx] || particles[cdf_idx].getWeight() == 0.0)) {
                cdf_idx++;
            }

            // if the resample particle weight is 0.0 (should only occur for the last part of the
            // cdf) then we take a
            // particle with non-zero weight..
            if (particles[cdf_idx].getWeight() == 0.0)
                resampledParticles[i] = new Particle(resampledParticles[i - 1]);
            else
                resampledParticles[i] = new Particle(particles[cdf_idx]);

            resampledParticles[i].setWeight(p_step);
        }

        particles = resampledParticles;
    }

    /**
     * notes on resampling mostly from: Probabilistic Robotics (Sebastian Thrun, Wolfram Burgard, Dieter Fox; Year: 2005)
     *  problems (sources of error) that can arise with particle filter, chapter 4.2.4 properties of the particle filter
     *  1. the finite number of particles leads to an approximation error
     *     - at resampling: non-normalized values drawm from M-dimensional space, but after normalization reside in space M-1
     *     => at larger dimension effect has less influence
     *  2. randomness introduced in resampling phase
     *  "This example hints at an important limitation of particle filters with immense
     *      practical ramifications. In particular, the resampling process induces a loss of di-
     *      versity in the particle population, which in fact manifests itself as approximation
     *      error. Such error is called variance of the estimator: Even though the variance of
     *      the particle set itself decreases, the variance of the particle set as an estimator of
     *      the true belief increases. Controlling this variance, or error, of the particle filter
     *      is essential for any practical implementation."
     *  => two strategies for variance reduction
     *  - reduce resampling period
     *         - especially when no movement, stop resampling (and also all integrations...)
     *      - "multiple measurements can integrated via multiplicativly updating the importance factor"
     *
     *      - "Resampling too often increases the risk of losing diversity"
     *       vs.
     *      - "If one samples too infrequently, many samples might be wasted in regions of low probability."
     *
     *      -how to determine:  measure the variance of the importance weights
     *        - "If all weights are identical, then the variance is zero and no resampling should be performed"
     *        - "If the weights are concentrated on a small number of samples, then the weight variance is high and resampling should be performed."
     *
     *  - use "low variance sampling"
     *     - instead of choosing the particles independently random of old particles, use a stochastic process
     *       - only a single random number
     *       - .... see implementation below
     *     - benefits
     *       1. the space of samples is covered more systematically(than independent random sampling)
     *       2. when having particles with equal weight(as in our case) no particles are lost
     *       3. complexity is O(N) ; for random resampling require to search particle using drawn
     *            random number adding complexity of log(N) ...resulting in O( N*log(N) ) for resampling process
     *
     *  3. "divergence of the proposal and target distribution"
     *     - "particles are generated from a proposal distribution that does not consider the measurement"
     *        - so we rely on the match the proposal and the target distribution
     *           - highly inaccurate sensors and very accurate motion => target dist. similar to proposal dist. => efficient particle filter
     *           - highly accurate sensors but inaccurate movement model =>
     *                proposal distribution never has sample in correct range => illconditioned , converging wrongly
     *     => strategies to overcome problem
     *     - add more noise than sensors actually have (we do it for length and direction of steps)
     *     - modify proposal distribution to incorporate measurement, ...?(later in book)
     *
     *  4. "particle deviation problem"
     *     - in higher dimensional spaces the density of particles becomes very sparse,
     *     therefore no particle might be around the "correct" region.
     *     - additionally at each (random)resampling step the probability that a particle in the correct region is removed is greater than 0.
     *       the longer it runs the more likely we remove one.
     *
     *     => add a small number of randomly generated particles after each resampling process
     *        - reduces deviation problem, but adds inaccuracy/error
     *        - very simple
     *        - measure of last resort - try to fix it with other means before
     *
     */

    /**
     * the lowVarianceSampler uses a cummulative distribution function to cover the samples more
     * systematically than a independent random samply can do. As in our case all particles have equal
     * weight no particles are lost at all. Additionally the complexity is O(N) which is also pretty nice.
     */
    public void lowVarianceSampler() {
        Particle[] newParticles = new Particle[numberOfParticles];
        Random r = new Random();
        double randomValue;
        double cummulativeWeightOldParticles = this.particles[0].getWeight();
        int indexOldParticles = 0;
        double cummulativeWeightNewParticles;
        double singleNewWeight = 1.0d / (double) numberOfParticles;

        randomValue = (r.nextDouble() ) * 1.0d / (double)numberOfParticles;
        Log.i("pF", String.format("rand: %f", randomValue));

        cummulativeWeightNewParticles = randomValue;
        for (int indexNew = 0; indexNew < numberOfParticles; indexNew++) {
            while ( (cummulativeWeightOldParticles < cummulativeWeightNewParticles) && (indexOldParticles <(numberOfParticles-1))  ) {
                indexOldParticles = indexOldParticles + 1;
                cummulativeWeightOldParticles += this.particles[indexOldParticles].getWeight();
            }

            Log.i("particleFilter", String.format("oldI %d, newI %d,  oldWeight %f   newWeight %f", indexOldParticles, indexNew, cummulativeWeightOldParticles , cummulativeWeightNewParticles));
            if (particles[indexOldParticles].getWeight() == 0.0) {
                newParticles[indexNew] = new Particle(this.particles[indexOldParticles -1]);
            } else {
                newParticles[indexNew] = new Particle(this.particles[indexOldParticles]);
            }
            newParticles[indexNew].setWeight(singleNewWeight);
            cummulativeWeightNewParticles += singleNewWeight;
        }
        this.particles = newParticles;
    }

    /**
     * randomly for samples that were invalidated randomly draws a new one
     * in our case not so ideal as,... see above
     */
    public void randomSampler() {
      Random r = new Random();
      for(int i = 0; i < numberOfParticles; i++) {
         if( particles[i].getWeight() < 0.01) {
             boolean validParticle = false;
             do
             {
                int index = r.nextInt(numberOfParticles);
                if(particles[index].getWeight() >= 0.01) {
                  particles[i] = new Particle(particles[index]);
                  validParticle = true;
                }
             } while(!validParticle);
         }
      }

    }

    /**
     * calculates the positon as from the median x and y values of all particles
     */
    public void updateCurrentPosition() {

        Position position = new Position();

        // take median of all particle positions
        double[] x = new double[numberOfParticles];
        double[] y = new double[numberOfParticles];
        for (int i = 0; i < numberOfParticles; i++) {
            x[i] = particles[i].getCurrentPosition().getX();
            y[i] = particles[i].getCurrentPosition().getY();
        }

        Arrays.sort(x);
        Arrays.sort(y);

        position = new Position(x[numberOfParticles / 2], y[numberOfParticles / 2]);
        currentPosition = position;


    }

}

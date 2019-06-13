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


//   public void startMovement(MotionEstimation motionEstimation) {
//
//       step_event_handler = new Handler();
//       step_event_handler.postDelayed(new Runnable() {
//           public void run() {
//               //if(motion_prediction_enabled) {
//               //String currentActivity = motionEstimation.estimate();
//               //MotionEstimation.Activity currentActivity = motionEstimation.estimate();
//               //mPredictionTextView.setText(String.format("Based on the accelerometer\n data it is likely that you are:\n %s", currentActivity.name()));
//               //TODO initialize particles ?
//
//               movement = motionEstimation;
//
//               if (motionEstimation) {
//                   if (currentActivity == SITTING || currentActivity == WALKING || currentActivity == JOGGING) {
//                       particleFilter.moveParticles(mAverageDegree);
//                       particleFilter.substitudeInvalidMoves();
//                       particleFilter.normalizeWeights();
//                       particleFilter.resampleParticles();
//                       particleFilter.updateCurrentPosition();
//
//                       floorMap.clearImage();
//                       floorMap.drawParticles(particleFilter.getParticles(), particleFilter.currentPosition);
//                   }
//                   if (currentActivity == STANDING) {
//
//                   }
//               }
//
//               prediction_event_update_handler.postDelayed(this, prediction_event_update_delay);
//           }
//       }, prediction_event_update_delay);
//
//   }

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
    * particles, that moved through walls are eliminated and substituted by a randomly chosen particle from the last iteration
    */
   public void substitudeInvalidMoves() {
      Random r = new Random();

      for(Particle particle : particles) {
         Line move = new Line(particle.getLastPosition(), particle.getCurrentPosition());

         for(Line wall : floor.walls) {
            if(move.intersects(wall)) {
                particle.setWeight(0.0d); //at first set to zero later resample
               break;
            }
         }
      }

      /*for(int i = 0; i < numberOfParticles; i++) {
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
      }*/
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

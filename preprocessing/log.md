

# benchmarking with sklearn.neighbors  KNeighborsClassifier
## WISDM dataset
- tests with window len 20, no overlap, train-ratio 0.7

- activities ["Jogging", "Walking", "Upstairs", "Downstairs", "Sitting", "Standing"]
    - features: ["x_max", "x_min", "x_mean", "y_max", "y_min", "y_mean", "z_max", "z_min", "z_mean", "n_max", "n_min", "n_mean"]
        - k=5     error-rate: 30%



- activities = ["Jogging", "Walking", "Sitting", "Standing"]
    - features: ["x_max", "x_min", "x_mean", "y_max", "y_min", "y_mean", "z_max", "z_min", "z_mean", "n_max", "n_min", "n_mean"]
        - k=5:    error-rate: 5.18%
	    - k=7:    error-rate: 5.178%
	    - k=9	  error-rate: 4.93%	
	    - k=11	  error-rate: 4.83%
	    - k=13	  error-rate: 4.72%
	    - k=17    error-rate: 4.61%
	    - k=19	  error-rate: 4.64%
	    - k=21    error-rate: 4.72%
	    - k=29	  error-rate: 4.68%
	
	- features: ["n_max", "n_min", "n_mean"]
        - k=5 error-rate: 17,75%
        
        
## own recordings
- activities = ["Jogging", "Walking", "Sitting", "Standing"]
    - features: ["x_max", "x_min", "x_mean", "y_max", "y_min", "y_mean", "z_max", "z_min", "z_mean", "n_max", "n_min", "n_mean"]
        - k=5:    error-rate: 1.063%
	    - k=7:    error-rate: %
	    - k=9	  error-rate: %	
	    - k=11	  error-rate: %
	    - k=13	  error-rate: %
	    - k=17    error-rate: 1.06%
	    - k=19	  error-rate: %
	    - k=21    error-rate: %
	    - k=29	  error-rate: %
	

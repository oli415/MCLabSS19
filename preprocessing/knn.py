# ideas from:
# https://machinelearningmastery.com/tutorial-to-implement-k-nearest-neighbors-in-python-from-scratch/
# https://www.dataquest.io/blog/k-nearest-neighbors-in-python/

import csv
import math

import numpy as np
import pandas as pd
from sklearn.neighbors import KNeighborsClassifier
from enum import Enum

# data is sampled each 100ms
# removed the last ";" in each line; e.g. vim: ":%normal $x"
# removed line 343420, as data was incomplete
filename = './Self_recorded/raw.csv'
#filename = './WISDM_ar_v1.1/WISDM_ar_v1.1_raw.csv'
#filename = './WISDM_ar_v1.1/WISDM_ar_v1.1_raw_smaller.csv'
features_filename = 'Self_recorded/features.csv'
#features_filename = 'WISDM_ar_v1.1/features_JogginWalkingSittingStanding_wholeset_allFeatures.csv'
#features_filename = 'WISDM_ar_v1.1/features_smaller.csv'
window_len = 20
window_overlap_facotor = 0.5
train_ratio = 0.7

use_preprocessed_features_file = False
#use_preprocessed_features_file = True

# other potential features: std-deviation, correlation coeff, energy
feature_columns = ["x_max", "x_min", "x_mean", "y_max", "y_min", "y_mean", "z_max", "z_min", "z_mean", "n_max", "n_min", "n_mean"]
#feature_columns = ["n_max", "n_min", "n_mean"]
identifier_columns = ["activity", "person_id"]
#activities = ["Jogging", "Walking", "Upstairs", "Downstairs", "Sitting", "Standing"]
activities = ["Jogging", "Walking", "Sitting", "Standing"]
activities = sorted(set(activities))  # unique and sorted
activities_map = d = dict([(y, x + 1) for x, y in enumerate(activities)])


def calculate_features(feature_frame, window_feature_index, user, activity, acceleration_window):

    feature_frame.loc[[window_feature_index], ['activity']] = activities_map[activity]
    feature_frame.loc[[window_feature_index], ['person_id']] = user

    feature_frame.loc[[window_feature_index], ['x_max']] = np.max(acceleration_window[:, 0])
    feature_frame.loc[[window_feature_index], ['x_min']] = np.min(acceleration_window[:, 0])
    feature_frame.loc[[window_feature_index], ['x_mean']] = np.mean(acceleration_window[:, 0])
    feature_frame.loc[[window_feature_index], ['y_max']] = np.max(acceleration_window[:, 1])
    feature_frame.loc[[window_feature_index], ['y_min']] = np.min(acceleration_window[:, 1])
    feature_frame.loc[[window_feature_index], ['y_mean']] = np.mean(acceleration_window[:, 1])
    feature_frame.loc[[window_feature_index], ['z_max']] = np.max(acceleration_window[:, 2])
    feature_frame.loc[[window_feature_index], ['z_min']] = np.min(acceleration_window[:, 2])
    feature_frame.loc[[window_feature_index], ['z_mean']] = np.mean(acceleration_window[:, 2])
    feature_frame.loc[[window_feature_index], ['n_max']] = np.max(acceleration_window[:, 3])
    feature_frame.loc[[window_feature_index], ['n_min']] = np.min(acceleration_window[:, 3])
    feature_frame.loc[[window_feature_index], ['n_mean']] = np.mean(acceleration_window[:, 3])
    #window_feature_index += 1


# normalize columns to have a mean of zero and standard deviation of 1
# the normalized values are returned and the mean and std value for each feature to be able to normalize live data
# todo export normalization coefficients
def normalize_features(feature_frame):
    feature_frame_normal = pd.DataFrame(0, index=np.arange(len(feature_frame)),
                     columns=identifier_columns + feature_columns)

    feature_mean_values = feature_frame.loc[:, feature_columns].mean()
    feature_std_values = feature_frame.loc[:, feature_columns].std()

    # identifier columns can't be normalized, just assign
    feature_frame_normal.loc[:, identifier_columns] = feature_frame.loc[:, identifier_columns]
    # feature columns are normalized
    feature_frame_normal.loc[:, feature_columns] = (feature_frame.loc[:, feature_columns] - feature_frame.loc[:, feature_columns].mean()) / feature_frame.loc[:, feature_columns].std()
    return feature_frame_normal
    #return feature_frame_normal, feature_mean_values, feature_std_values


def euclidean_distance(instance1, instance2, length):
    distance = 0
    for x in range(len(feature_columns)):
        distance += pow((instance1[0][x] - instance2[0][x]), 2)
    #return np.sum(np.square(current_acceleration_window[in_window_offset, 0:3]))
    return math.sqrt(distance)


def get_neighbors(training_set, test_instance, k):
    distances = np.zeros(len(training_set))
    length = len(test_instance) - 1
    for x in range(len(training_set)):
        dist = euclidean_distance((test_instance.loc[:, feature_columns]).to_numpy(), (training_set.loc[[x], feature_columns]).to_numpy(), length)
        distances[x] = dist
    distances_sort_arg = np.argsort(distances)
    train_classes_list = training_set.loc[:, ["activity"]]
    train_classes_list = train_classes_list.to_numpy()
    neighbors = train_classes_list[ distances_sort_arg[:k]]
    print(type(neighbors))
    print(neighbors)
    return neighbors


def get_dominating_neighbor_class(neighbors):
    class_votes = np.zeros(len(activities))
    for x in range(len(neighbors)):
        response = int(neighbors[x])
        class_votes[response] += 1
    #print(type(class_votes))
    #print(class_votes)
    indexes_of_sorted_claesses = np.argsort(class_votes)
    #sorted_votes = sorted(class_votes.iteritems(), key=operator.itemgetter(1), reverse=True)
    print(indexes_of_sorted_claesses[0])
    return indexes_of_sorted_claesses[0]


def get_accuracy(test_set, predictions):
    correct = 0
    for x in range(len(test_set)):
        if test_set[x][-1] == predictions[x]:
            correct += 1
    return (correct/float(len(test_set))) * 100.0


# todo overlapping windows
def create_feature_set():
    row_count = 0
    with open(filename, 'r') as csvDataFile:
        #global row_count
        row_count = sum( 1 for line in csvDataFile)  # fileObject is your csv.reader
    with open(filename, 'r') as csvDataFile:
        lines = csv.reader(csvDataFile)
        print("initial num. rows: {}".format(row_count))
        window_count = 0
        in_window_offset = 0
        current_user = 0
        current_activity = ""
        #window_features = np.zeros((int(row_count/window_len), 12))  # todo size dynamically, regard overlapping, in fact is a bit smaller
        frame_columns = identifier_columns + feature_columns
        window_features = np.zeros((int(550000), len(frame_columns)))  # todo size dynamically, regard overlapping, in fact is a bit smaller
        window_feature_frame = pd.DataFrame(window_features, columns=frame_columns)
        window_feature_index = 0
        current_acceleration_window = np.zeros((window_len, 4), dtype=float)

        for i, row in enumerate(lines):
            if i != 0 and (i*100) % row_count == 0:
                print((i*100)/row_count)
            if len(row) != 6:
                print("error: input columns")
                continue
            if row[1] not in activities:
                current_activity = row[1]
                continue
            if current_user != row[0] or current_activity != row[1]:
                #todo delete current set start new
                current_user = row[0]
                current_activity = row[1]
                in_window_offset = 0
                #samples_in_current_window_count = 0

            # assign the x,y,z acceleration values
            current_acceleration_window[in_window_offset][:3] = row[3:6]
            # squared absolute value: direction independed n as  sum of squares(of x,y,z)
            current_acceleration_window[in_window_offset][3] = np.sum(np.square(current_acceleration_window[in_window_offset, 0:3]))
            in_window_offset += 1

            # once window_len values are accumulated, calculate features, todo has to be modified to calculate features from overlapping window-frame
            if in_window_offset >= window_len:
                calculate_features(window_feature_frame, window_feature_index , current_user, current_activity, current_acceleration_window)
                window_feature_index += 1
                in_window_offset = 0
                #in_window_offset -= int(window_len * window_overlap_facotor)

    print("num. feature rows: {}".format(window_feature_index))
    window_feature_frame = window_feature_frame[:window_feature_index+1]
    window_feature_frame.to_csv(features_filename)


def train_and_classify_manually():
    window_feature_frame = pd.read_csv(features_filename)
    norm, norm_mean, norm_std = normalize_features(window_feature_frame)
    print("then")

    train_test_split_index = math.floor(len(norm)*train_ratio)
    train = norm[:train_test_split_index]
    test = norm[train_test_split_index:]
    predictions = np.zeros(len(test))

    k = 5
    #for x in range(len(test)):
    for i, id in enumerate(test.index):
        neighbors = get_neighbors(train.loc[:, :], test.loc[[id], :], k)
        predictions[i] = get_dominating_neighbor_class(neighbors)
#        print('> predicted=' + repr(result) + ', actual=' + repr(
#            testSet[x][-1]))
    accuracy = get_accuracy(test, predictions)
    print('Accuracy: ' + repr(accuracy) + '%')


def train_and_classify():
    print("afer")
    window_feature_frame = pd.read_csv(features_filename)
    norm = normalize_features(window_feature_frame)
    print("then")
    # todo store normalization mean / std, that we can also normalize the live data
    train_test_split_index = math.floor(len(norm)*train_ratio)
    train = norm[:train_test_split_index]
    test = norm[train_test_split_index:]

    # evaluate based on the  five closest neighbors.
    knn = KNeighborsClassifier(n_neighbors=5)

    # Fit the model on the training data.
    y_col = ["activity"]

    knn.fit(train.loc[:, feature_columns], np.ravel(train.loc[:, y_col].values))
    print(train.loc[:, feature_columns])
    print(train.loc[:, y_col])
    print(test.loc[:, feature_columns])


    # predict on the test set using the fit model.
    predictions = knn.predict(test.loc[:, feature_columns])
    print("predictions:")
    print(type(predictions))
    print(predictions)

    # Get the actual values for the test set.
    print("actual:")
    actual = test.loc[:, y_col].values
    print(type(actual))
    print(actual)


    # Compute the mean squared error of our predictions.
    #mse = (((predictions - actual) ** 2).sum()) / len(predictions)
    errors = 0
    for i in range(len(predictions)):
        #print("pre{}".format(predictions[i]))
        #print("act{}".format(actual[i]))
        if predictions[i] != actual[i]:
            print("pred:{}   act:{}".format(predictions[i], actual[i]))
            errors += 1

    print("{} error out of {} -> error_rate:{}".format(errors, len(test), errors/len(actual)))


print(activities_map)
if not use_preprocessed_features_file:
    create_feature_set()

#train_and_classify_manually()
train_and_classify()


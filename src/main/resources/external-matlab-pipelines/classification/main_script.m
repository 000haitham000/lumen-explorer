clear all
clc
rootDir = 'E:\Dropbox\Harmony\3D Harmony counts\Test Images\labeled images\preclassify';
[files, pred, score] = lumenClassify(rootDir)
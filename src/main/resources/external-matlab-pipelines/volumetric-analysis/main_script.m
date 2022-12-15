clear all
clc
lumenFilePaths = {};
i = 1;
file = sprintf('lumen-boundaries-%02d.txt', i);
while exist(file, 'file')
    lumenFilePaths{i} = file;
    i = i + 1;
    file = sprintf('lumen-boundaries-%02d.txt', i);
end
[allVolumes allAreas] = calculateVolumeAndArea(lumenFilePaths, true, true)

function points = getAllPoints(layers)
%getAllLayers Get all points in the layers in order
points = [];
for i = 1 : size(layers, 2)
    layerPoints = vec2mat(cell2mat(layers{i}), 3);
    points = [points; layerPoints];
end


function layers = groupIntoLayers(points)
%groupIntoLayers Groups points into layers based on the z coordinates
layers = {};
for i = 1 : size(points, 1)
    currentPoint = points(i, :);
     if i == 1
        layer{1} = currentPoint;
        counter = 2;
    else
        previousPoint = points(i - 1, :);
        if currentPoint(3) ~= previousPoint(3)
            layers{end + 1} = layer;
            layer = {};
            layer{1} = currentPoint;
            counter = 2;
        else
            layer{counter} = currentPoint;
            counter = counter + 1;
        end
        if i == size(points, 1)
            layers{end + 1} = layer;
        end
    end
end
end


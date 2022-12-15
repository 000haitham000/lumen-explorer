function finalLayer = composeExtraLayer(layer, scalingFactor, zDiff)
%composeExtraLayer Create a less resolution smaller layer above or below
%   the inpiut layer.
% Remove half the number of points
if size(layer, 2) > 2
    oneLayerUp = {};
    index = 1;
    for i = 1 : 2 : size(layer, 2)
        oneLayerUp{index} = layer{i};
        index = index + 1;
    end
end
% Cut off the z coordinate of the points as all the following
% transformations will be done on the level of the plane i.e. in 2D.
original3d = vec2mat(cell2mat(layer), 3);
original2d = [original3d(:, 1), original3d(:, 2)];
new3d = vec2mat(cell2mat(oneLayerUp), 3);
new2d = [new3d(:, 1), new3d(:, 2)];
% Scale the shape down by half
scalingTransformtion = affine2d( ...
    [scalingFactor 0 0; 0 scalingFactor 0; 0 0 1]);
scaled = transformPointsForward(scalingTransformtion, new2d);
% Claculate the barycenters of the points before and after scaling
originalBaryCenter = sum(original2d) ./ size(original2d,1);
scaledBaryCenter = sum(scaled) ./ size(scaled,1);
% Create the translation transformation based on the difference between the
% two barycenters i.e. bring the new layer closer to the original layer so
% that they both have the same barycenter.
translationTransformation = affine2d( ...
    [1 0 0; ...
    0 1 0; ...
    originalBaryCenter(1) - scaledBaryCenter(1) ...
    originalBaryCenter(2) - scaledBaryCenter(2) ...
    1]);
translated = transformPointsForward(translationTransformation, scaled);
% Add the z ccordinate to the new layer so that it is located above or
% below (depending on the argument input) the original layer.
finalArr = [translated, new3d(:, 3) + zDiff];
finalLayer = num2cell(finalArr, 2)';
end


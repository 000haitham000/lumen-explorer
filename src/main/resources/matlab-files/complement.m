function allLayers = complement(layers, scalingFactor, zDiff)
%complementedLayers Add two layers to each end of the figure (top and
%   bottom) to make it more shperical.
%% Extra bottom layer(s)
% Extract the original (already existing) bottom layer
originalBottomLayerArr = vec2mat(cell2mat(layers{1}), 3);
% Add the new bottom single-point layer (which should be directly below 
% the barycanter of the original bottom layer)
newBottomLayers = {};
if size(originalBottomLayerArr, 1) > 2
    originalBottomLayerBaryCenter = ...
        sum(originalBottomLayerArr) ./ size(originalBottomLayerArr, 1);
    if size(originalBottomLayerArr, 1) > 5
        zDiff1 = -1.5 * zDiff;
        zDiff2 = -1 * zDiff;
    else
        zDiff1 = -0.5 * zDiff;
    end
    originalBottomLayerBaryCenter(:, 3) = ...
        originalBottomLayerBaryCenter(:, 3) + zDiff1;
    newBottomLayers{1} = {originalBottomLayerBaryCenter};
    % If necessary, add another layer between the new bottom layer 
    % (single-point layer) and the original bottom layer.
    if size(originalBottomLayerArr, 1) > 5
        newBottomLayers{2} = composeExtraLayer(layers{1}, scalingFactor, zDiff2);
    end
end
%% Extra topmost layers
% Extract the original (already existing) topmost layer
originalTopmostLayerArr = vec2mat(cell2mat(layers{end}), 3);
% Add the new topmost single-point layer (which should be directly above
% the barycanter of the original topmost layer)
newTopLayers = {};
if size(originalTopmostLayerArr, 1) > 2
    originalTopmostLayerBaryCenter = ...
        sum(originalTopmostLayerArr) ./ size(originalTopmostLayerArr, 1);
    if size(originalTopmostLayerArr, 1) > 5
        zDiff1 = 1.5 * zDiff;
        zDiff2 = zDiff;
    else
        zDiff1 = 0.5 * zDiff;
    end
    originalTopmostLayerBaryCenter(:, 3) = ...
        originalTopmostLayerBaryCenter(:, 3) + zDiff1;
    newTopLayers{1} = {originalTopmostLayerBaryCenter};
    % If necessary, add another layer between the new topmost layer 
    % (single-point layer) and the original topmost layer.
    if size(originalTopmostLayerArr, 1) > 5
        newTopLayers{2} = composeExtraLayer(layers{end}, scalingFactor, zDiff2);
    end
end
%% Combine all layers (original and new)
allLayers = {};
if size(newBottomLayers, 2) > 0
    for i = 1 : size(newBottomLayers, 2)
        allLayers{end + 1} = newBottomLayers{i};
    end
end
for i = 1 : size(layers, 2)
    allLayers{end + 1} = layers{i};
end
if size(newTopLayers, 2) > 0
    for i = size(newTopLayers, 2) : -1 : 1
        allLayers{end + 1} = newTopLayers{i};
    end
end
% hold on
% scatter(original2d(:, 1), original2d(:, 2), 20, 'MarkerFaceColor', [1, 0, 0]);
% scatter(originalBaryCenter(1), originalBaryCenter(2), 40);
% % scatter(scaled(:, 1), scaled(:, 2), 20, 'MarkerFaceColor', [0, 1, 0]);
% % scatter(scaledBaryCenter(1), scaledBaryCenter(2), 40);
% scatter(translated(:, 1), translated(:, 2), 20, 'MarkerFaceColor', [0, 0, 1]);
% scatter(translatedBaryCenter(1), translatedBaryCenter(2), 40);
% hold off
end


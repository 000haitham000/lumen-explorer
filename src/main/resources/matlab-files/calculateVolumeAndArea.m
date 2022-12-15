function [allVolumes, allAreas] = calculateVolumeAndArea( ...
    filePathList, ...
    savePlotEach, ...
    savePlotAll)
%calculateVolumeAndArea returns volumes and surface areas of the argument
%shapes.
%   The function returns the surface area as a by-product.
    
    timeStep = 0.1;
    rotationStep = 0.1;
    scalingFactor = 0.5;
    zDiff = 3;

%     volumes = zeros(length(filePathList), 1);
%     areas = zeros(length(filePathList), 1);
    allPoints = {length(filePathList)};
    originalPoints = {length(filePathList)};
    pointsDiff = {length(filePathList)};
    allShapes = {length(filePathList)};
%     originalShapes = {length(filePathList)};
    allVolumes = zeros(length(filePathList), 1);
    allAreas = zeros(length(filePathList), 1);
    for i = 1 : length(filePathList)
        % Read current lumen points from the respective file
        originalPoints{i} = load(filePathList{i});
        % Group them into layers
        layers = groupIntoLayers(originalPoints{i});
        % Complement the original layers with extra layers top and down for
        % a better shperoid-like shape
        complementedLayers = complement(layers, scalingFactor, zDiff);
        % Get all the points (original + complemented)
        allPoints{i} = getAllPoints(complementedLayers);
        % Git the difference between the original and the complemented ones
        pointsDiff{i} = getPointsDiff(allPoints{i}, originalPoints{i});
        % Create shape
        allShapes{i} = alphaShape(allPoints{i}, 20, 'HoleThreshold', 100);
%         originalShapes{i} = alphaShape(originalPoints{i}, 20, 'HoleThreshold', 100);
        % Compute volume
        allVolumes(i) = volume(allShapes{i});
        % Compute surface area
        allAreas(i) = surfaceArea(allShapes{i});
    end
    % Plot the current lumen in an independent figure
    if savePlotEach
        for i = 1 : length(allPoints)
            oPoints = originalPoints{i};
%             % Plot the original shape
%             f = figure;
%             view([20 25]);
%             grid on
%             hold on
%             plot(originalShapes{i});
%             scatter3(oPoints(:, 1), oPoints(:, 2), oPoints(:, 3), 70, 'MarkerFaceColor', [0.5, 0.5, 0.5], 'MarkerEdgeColor', [0, 0, 0]);
%             hold off
            % Plot the augmented shape
            f = figure;
            view([20 25]);
            grid on
            hold on
            plot(allShapes{i});
            scatter3(oPoints(:, 1), oPoints(:, 2), oPoints(:, 3), 70, 'MarkerFaceColor', [0.5, 0.5, 0.5], 'MarkerEdgeColor', [0, 0, 0]);
            if size(pointsDiff{i}, 1) > 0
                pDiff = pointsDiff{i};
                scatter3(pDiff(:, 1), pDiff(:, 2), pDiff(:, 3), 70, 'MarkerFaceColor', [1, 1, 0.4], 'MarkerEdgeColor', [0, 0, 0]);
            end    
            hold off
            % Write the figure to files (.fig and .png)
            [filePath, fileName, fileExt] = fileparts(filePathList{i});
            if isempty(filePath)
                finalPath = fileName;
            else
                finalPath = strcat([filePath '/'], fileName);
            end
            saveas(gcf, finalPath, 'fig');
            saveas(gcf, finalPath, 'png');
            close(f)
        end
    end
     % Add the current lumen to the combined plot
    if savePlotAll && ~isempty(filePathList)
        f = figure;
        hold on
        for i = 1 : length(allPoints)
            plot(allShapes{i});
%             points = allPoints{i};
%             faces = allFaces{i};
%             % Plot the surface
%             trisurf(faces, points(:, 1), points(:, 2), points(:, 3), ...
%                 'facecolor', 'c', ...
%                 'edgecolor', 'b')
        end
        hold off
        grid on
        view(40, 40)
        % Write the figure to files (.fig and .png)
        [filePath, fileName, fileExt] = fileparts(filePathList{1});
        if isempty(filePath)
            finalPath = 'all_lumens';
        else
            finalPath = strcat([filePath '/'], 'all_lumens');
        end
        saveas(f, finalPath, 'fig');
        saveas(f, finalPath, 'png');
        close(f)
    end
end

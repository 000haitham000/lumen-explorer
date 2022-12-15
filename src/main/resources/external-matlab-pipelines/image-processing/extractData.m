% Identifies potentials lumens in an image, cut each of them into
% a separate image, match them with the manually identified lumens and
% save each of them in the directory that matches its label. The funtions
% takes the following parameters:
%   cellImagePath: cells channel of the image under consideration
%   wallImagePath: walls channel of the image under consideration
%	outputPath: output directory to shich final images will be stored
%   linearFusionWeight: linear fusion parameter(p). 0 <= p <= 1. The value
%   of p is directly proportional to how superior thr first image in the
%   fusion.
%	coordinates: coordinates of the manually identified lumens on a scaled
%	version of the image
%	taggingScalingFactor: the scaling factor applied by the tagging
%	software to the image before allowing the user to manually tag lumens
%	diskSizeStart: first value of the morphological opening/closing disk
%   size
%	diskSizeStep: step of the morphological opening/closing disk size
%	diskSizeEnd: last value of the morphological opening/closing disk size
%	binarizationThreshold: intensisty threshold used to convert the
%	grayscale image to a binary image
%	openingSize: minimum size of a component. All component with smaller
%	number of pixels are removed to clear the image
%	subImageMarginThickness: margin added to the potential lumen in the
%	hope of capturing its surroundings
% extractTrainingData( ...
%     cellImagePath, ...
%     wallImagePath, ...
%     outputPath, ...
%     linearFusionWeight, ...
%     coordinates, ...
%     taggingScalingFactor, ...
%     diskSizeStart, ...
%     diskSizeStep, ...
%     diskSizeEnd, ...
%     binarizationThreshold, ...
%     openingSize, ...
%     subImageMarginThickness)
function subImageInfoList = extractData( ...
    mode, ...
    cellImagePath, ...
    wallImagePath, ...
    outputPath, ...
    linearFusionWeight, ...
    coordinates, ...
    taggingScalingFactor, ...
    diskSizeStart, ...
    diskSizeStep, ...
    diskSizeEnd, ...
    binarizationThreshold, ...
    openingSize, ...
    subImageMarginAreaPercent, ...
    subImageMarginDiskMaxRadius, ...
    displayAll)
    if strcmpi(mode, 'train')
        % Create output directories
        lumenDir = [outputPath '/lumen'];
        noLumenDir = [outputPath '/notlumen'];
        if exist(lumenDir, 'dir') ~= 7
            mkdir(lumenDir)
        end
        if exist(noLumenDir, 'dir') ~= 7
            mkdir(noLumenDir)
        end
    elseif strcmpi(mode, 'preclassify')
        disp('Pre-Classification started')
        preclassifyDir = [outputPath '/preclassify'];
        if exist(preclassifyDir, 'dir') ~= 7
            mkdir(preclassifyDir)
        end
    else
        error('mode must be either ''train'' or ''preclassify''');
    end    
    % Extract file name from path
    [filepath, fileName, fileExt] = fileparts(cellImagePath);
    % Read images and fuse them
    originalCells = im2double(imadjust(imread(cellImagePath)));
    originalWalls = im2double(imadjust(imread(wallImagePath)));
    fusionMethod = struct('name', 'linear', 'param', linearFusionWeight);
    originalFused = wfusmat(originalCells, originalWalls, fusionMethod);
    % Display an RGB version of the grayscale image (to allow coloring
    % components later as desired)
    rgbImage = cat(3, originalFused, originalFused, originalFused);
    R = rgbImage(:,:,1);
    G = rgbImage(:,:,2);
    B = rgbImage(:,:,3);
    % Create the scaling matrix
    if strcmpi(mode, 'train') == 1
        taggingScalingMatrix = diag([taggingScalingFactor taggingScalingFactor]);
        taggingDescalingMatrix = inv(taggingScalingMatrix);
        descaledCoordinates = zeros(size(coordinates, 1), size(coordinates, 2));
        if size(descaledCoordinates, 1) > 0 && size(descaledCoordinates, 2) == 2
            for i = 1 : size(coordinates, 1)
                descaledCoordinates(i, :) = ...
                    [coordinates(i, 1) coordinates(i, 2)] ...
                    * taggingDescalingMatrix;
            end
        else
            descaledCoordinates = [];
        end
    end
    % Display the original image before any further processing
    if displayAll
        figure
        imshow(rgbImage)
    end
    %% Create a binary image to hold the final components
    candidateLumens = zeros(size(originalCells));
%     % Impose manually identified lumen coordinates and save the image.
%     % This step is intended to check that the coordinates we are
%     % sending from Java are dealt with exactly as if we type them in
%     % the MATLAB script directly
%     normalizedCopy = normalized;
%     for j = 1 : size(coordinates, 1)
%         descaledCoordinates = ...
%             [coordinates(j, 1) coordinates(j, 2)] ...
%             * taggingDescalingMatrix;
%         lumenX = round(descaledCoordinates(1));
%         lumenY = round(descaledCoordinates(2));
%         normalizedCopy(lumenY - 10 : lumenY + 10, lumenX - 10 : lumenX + 10) = 1;
%     end
%     imwrite(normalizedCopy, [outputPath '/test_coordinates.png']);
    % Apply top hat filter
    se = strel('disk', 25);
    contrasted = imadjust(imtophat(originalCells, se));
    for diskSize = diskSizeStart : diskSizeStep : diskSizeEnd
        %% Image Processing Pipeline
        % Binarize
        contrastedBw = imbinarize(contrasted, binarizationThreshold);
        % Apply morphological closing
        se = strel('disk', diskSize);
        contrastedBwClosed = imclose(contrastedBw, se);
        % Remove too small components
        contrastedBwClosedCleaned = bwareaopen(contrastedBwClosed, openingSize);
        if displayAll
            figure
            imshow(contrastedBwClosedCleaned)
        end
        % Complement black and white
        contrastedBwClosedCleanedInverted = imcomplement(contrastedBwClosedCleaned);
        %% Color components
        % Fill components
        cc = bwconncomp(contrastedBwClosedCleanedInverted, 8);
        numPixels = cellfun(@numel, cc.PixelIdxList);
        [biggest, idx] = max(numPixels);
        for i = 1 : length(numPixels)
            if i ~= idx
                candidateLumens(cc.PixelIdxList{i}) = 1;
                % Color identified lumens
                R(cc.PixelIdxList{i}) = 0;
                G(cc.PixelIdxList{i}) = 1.0;
                B(cc.PixelIdxList{i}) = 0;
                if displayAll
                    rgbImage = cat(3, R, G, B);
                    imshow(rgbImage);
                    if strcmpi(mode, 'train') == 1
                        plotCoordinates(descaledCoordinates);
                    end
                end
            end
        end
        % Outline components
        [boundaries, L, n, A] = bwboundaries(contrastedBwClosedCleanedInverted, 8);
        connCompBoundaries = boundaries(1:n);
        % Find the index of the largest boundary (in order to exclude it)
        idx = 1;
        for i = 2 : n
            if size(connCompBoundaries{i}, 1) > size(connCompBoundaries{idx}, 1)
                idx = i;
            end
        end
        if displayAll
            hold on
            for k = 1 : length(connCompBoundaries)
                if k ~= idx
                    thisBoundary = boundaries{k};
                    plot(thisBoundary(:,2), thisBoundary(:,1), 'r', 'LineWidth', 2);
                end
            end
            hold off
        end
    end
    % hold off
    % % Display the detected lumens. The algorithm - up to this point - is
    % % expected to have a high false positive rate. A classifier will be
    % %used subsequently to identify actual lumens.
    % figure(2)
    % imshow(candidateLumens)
    % Idetify potential lumens
    cc2 = bwconncomp(candidateLumens);
    % Outline components
    [boundaries, L, n, A] = bwboundaries(candidateLumens, 8);
    exactConnCompBoundaries = boundaries(1:n);
    if displayAll
        figure
        imshow(candidateLumens)
        hold on
        for k = 1 : length(exactConnCompBoundaries)
            thisBoundary = boundaries{k};
            plot(thisBoundary(:,2), thisBoundary(:,1), 'r', 'LineWidth', 2);
        end
        hold off
    end
    % Initialize the cell array that will contain the separate potential
    % lumen images. It might be useful to keep them in a cell array just in
    % case we need them later.
    colorPotentialLumenList = cell(length(cc2.PixelIdxList), 1);
    % Initialize image counters
    lumenCount = 0;
    notLumenCount = 0;
    blindCounter = 0;
    % hold on
    % Calculate the box surrounding each component
    for i = 1 : length(cc2.PixelIdxList)
        if displayAll
            disp([num2str(i) ' of ' num2str(length(cc2.PixelIdxList))]);
        end
        % Create a binary image highlighting the potential lumen under
        % investigation only
        tempImage = zeros( ...
            size(candidateLumens, 1), size(candidateLumens, 2));
        tempImage(cc2.PixelIdxList{i}) = 1;
        % Dilate the binary image to capture a slightly larger border,
        % in the hope of capturing the surroundings (e.g. cells) of the
        % potential lumen.
        radius = round( ...
            sqrt(subImageMarginAreaPercent ...
            * length(cc2.PixelIdxList{i}) / pi));
        if radius > subImageMarginDiskMaxRadius
            radius = subImageMarginDiskMaxRadius;
        end
        se = strel('disk', radius);
        tempImageDilated = imdilate(tempImage, se);
        % Check if this potential lumen was manually identified
        % Notice that in Java, X represents the horizontal axis, Y
        % represents the vertical axis and the top left corner point is
        % the point of origin. MATLAB uses different semantics however.
        % In MATLAB, an image is a matrix (let's restrict our
        % discussion to grayscale images without loss of generality).
        % The pixel (X, Y) in a MATLAB image is the pixel at row X and
        % column Y in the matrix, which means that X in MATLAB
        % represents the vertical axis and Y represents the horizontal
        % axis, while keeping the point of origin at the top left
        % corner of the image/matrix. Notice in the following few lines
        % the first coordinate sent from Java represents the horizontal
        % coordinate and the second represents the vertical coordinate.
        % This is why this script avoids using the X/Y terminology and
        % use instead the terms "horizontal" and "vertical".
        if strcmpi(mode, 'train') == 1
            lumenDetected = false;
            if size(descaledCoordinates, 1) > 0 && size(descaledCoordinates, 2) == 2
                for j = 1 : size(descaledCoordinates, 1)
                    lumenHorizontalCoordinate = round(descaledCoordinates(j, 1));
                    lumenVerticalCoordinate = round(descaledCoordinates(j, 2));
                    if lumenVerticalCoordinate < 0 ...
                            || lumenVerticalCoordinate > size(tempImageDilated, 1) ...
                            || lumenHorizontalCoordinate < 0 ...
                            || lumenHorizontalCoordinate > size(tempImageDilated, 2)
                        disp('Manually labeled lumen outside image boundaries.')
                        continue;
                    end
                    if tempImageDilated(lumenVerticalCoordinate, ...
                            lumenHorizontalCoordinate) == 1
                        lumenDetected = true;
                        break;
                    end
                end
            end
        end
        % Identify the connected component in the dialted image (only one
        % connected component should be there)
        cc3 = bwconncomp(tempImageDilated);
        % Convert form index to subscript coordinates i.e. (x,y)
        [rows, columns] = ind2sub( ...
            [size(tempImageDilated, 1) size(tempImageDilated, 2)], ...
            cc3.PixelIdxList{1});
        % Detect the cut limits on both the horizontal and vertical axes
        verticalMin = min(rows);
        horizontalMin = min(columns);
        verticalMax = max(rows);
        horizontalMax = max(columns);
        % Make sure the sub-image is square
        horizontalCenter = horizontalMin + (horizontalMax - horizontalMin)/2;
        verticalCenter = verticalMin + (verticalMax - verticalMin)/2;
        horizontalDiff = horizontalMax - horizontalMin;
        verticalDiff = verticalMax - verticalMin;
        if horizontalDiff > verticalDiff
            side = horizontalDiff / 2;
        else
            side = verticalDiff / 2;
        end
        up = round(verticalCenter - side);
        down = round(verticalCenter + side);
        left = round(horizontalCenter - side);
        right = round(horizontalCenter + side);
        % If the sub-image cross the original image boundaries, keep
        % shrinking it until it is completely contained inside the
        % original(large) image.
        while up < 1 || down > size(candidateLumens, 1) ...
                || left < 1 || right > size(candidateLumens, 2)
            up = up + 1;
            down = down - 1;
            left = left + 1;
            right = right -1;
        end
        % Becuase of the rounding performed while calculating up, down, 
        % left and right, the final sub-image may be one pixel short in one
        % dimension. In such case, if the size of the potential lumen is
        % already too small, the shrinkage may result in invalid situations
        % where up > down (i.e. a -ve height sub-image) or left > right
        % (i.e. a -ve width image). In order to avoid this artificiality,
        % and since these tiny components do not represent lumens by any
        % means, we ignore them completely.
        if up <= down && left <= right
            % Cut the corresponding area from the normalized image
            potentialLumenDilated = originalFused;
            potentialLumenDilated(~tempImageDilated) = 0;
            if displayAll
                figure
                hold on
                imshow(potentialLumenDilated)
                if strcmpi(mode, 'train') == 1
                    plotCoordinates(descaledCoordinates);
                end
                hold off
            end
            % Form a small image containing only this lumen and its
            % surroundings
            grayPotentialLumenSubImage =  potentialLumenDilated( ...
                up : down, left : right);
            % Resize the image to fit AlexNet input size
            grayPotentialLumenResized = imresize( ...
                grayPotentialLumenSubImage, ...
                [227 227]);
            % Create an RGB image of the grayscale image (AlexNet takes RGB
            % images)
            colorPotentialLumenList{i} = cat( ...
                3, ...
                grayPotentialLumenResized, ...
                grayPotentialLumenResized, ...
                grayPotentialLumenResized);
            % In "train" mode put each sub-image into its repesctive
            % directory, however in "preclassify" mode add all of them to
            % the same directory and create an info file for each
            % sub-image.
            if strcmpi(mode, 'train') == 1
                % If the potential lumen matches an already manually identified
                % lumen, write it to the "lumen" directory, if not write it to
                % the "notlumen" directory.
                if lumenDetected
                    lumenCount = lumenCount + 1;
                    lumenFileName = [ ...
                        sprintf('%02d-', lumenCount) ...
                        fileName ...
                        sprintf( ...
                            '%04dx%04d', ...
                            lumenHorizontalCoordinate, ...
                            lumenVerticalCoordinate)];
                    path = [outputPath '/lumen/' lumenFileName '.png'];
                else
                    notLumenCount = notLumenCount + 1;
                    lumenFileName = [ ...
                        sprintf('%02d-', notLumenCount) ...
                        fileName];
                    path = [outputPath '/notlumen/' lumenFileName '.png'];
                end
            elseif strcmpi(mode, 'preclassify') == 1
                splits = strsplit(fileName, '-');
                blindCounter = blindCounter + 1;
                lumenFileName = [ ...
                        splits{1} ...
                        sprintf('-%02d', blindCounter)];
                path = [preclassifyDir '/' lumenFileName '.png'];
            end
            imwrite(colorPotentialLumenList{i}, path);
            % Convert form index to subscript coordinates i.e. (x,y)
            [rows, columns] = ind2sub( ...
                [size(tempImage, 1) size(tempImage, 2)], ...
                cc2.PixelIdxList{i});
            corners = [left up; right up; right down; left down];
            subImageInfoList{i} = {path, [rows, columns], exactConnCompBoundaries{i}, corners};
            % Create the info file for each sub-image if in "preclassify"
            % mode.
            if strcmpi(mode, 'preclassify') == 1
                infoFile = fopen( ...
                    [preclassifyDir '/' lumenFileName '.txt'], 'w');
                fprintf(infoFile, '---\n');
                fprintf(infoFile, '%s\n', cellImagePath);
                fprintf(infoFile, '%s\n', wallImagePath);
                fprintf(infoFile, '%s\n', path);
                fprintf(infoFile, '---\n');
                tempBoundary = exactConnCompBoundaries{i};
                fprintf(infoFile, '%04d %04d\n', ...
                    [tempBoundary(:, 2)'; tempBoundary(:, 1)']);
                fprintf(infoFile, '---\n');
                fprintf(infoFile, '%04d %04d\n', [corners(:, 1)'; corners(:,2)']);
                fclose(infoFile);
            end
        end       
    end
    % Even if there is no sub-images, the cell array needs to be
    % initialized (as an empty cell array) and returned to Java
    if exist('subImageInfoList') ~= 1
        subImageInfoList = {};
    end
    % Display the final result of everything
    if displayAll
        figure
        % Show original normalized image
        imshow(originalFused);
        hold on
        for i = 1 : size(subImageInfoList, 2)
            body = subImageInfoList{i}{2};
            boundary = subImageInfoList{i}{3};
            corners = subImageInfoList{i}{4};
            cornersClosed = [corners; corners(1,:)];
            % Notice while plotting the body and the boundary that plot and
            % scatter commands tales the horizontal parameter vector first
            % then the vertical parameter, while the data stored in
            % subImageInfo is the other way around (vertical then
            % horizontal). This is becuase these data are the outcomes of 
            % bwconncomp and bwboundaries routines. Notice how the first
            % two arguments of plot and scatter in the following line are
            % reversed.
            % Plot body
            scatter(body(:, 2), body(:, 1), 1, 'MarkerFaceColor', 'g', 'MarkerEdgeColor', 'g');
            % Plot boundaries
            plot(boundary(:, 2), boundary(:, 1), 'Color', 'r', 'LineWidth', 2);
            % Plot surrounding boxes
            plot(cornersClosed(:,1), cornersClosed(:,2), 'Color', 'b');
        end
        % Impose the manually labeled coordinates
        if strcmpi(mode, 'train') == 1
            scatter( ...
                descaledCoordinates(:, 1), ...
                descaledCoordinates(:, 2), ...
                'MarkerFaceColor', 'w', ...
                'MarkerEdgeColor', 'k');
        end
        hold off
    end
end

function pointsDiff = getPointsDiff(points1,points2)
%getPointsDiff Get the points in point1 that do not exist in point2
rowsToBeRemoved = [];
for i = 1 : size(points1, 1)
    for j = 1 : size(points2, 1)
        if points1(i, :) == points2(j, :)
            rowsToBeRemoved = [rowsToBeRemoved; i];
            break;
        end
    end
end
pointsDiff = points1;
pointsDiff(rowsToBeRemoved, :) = [];
end


function [files, prediction, score] = lumenClassify(rootPath)
    load lumenNet;
    lumenImageDataStore = imageDatastore( ...
        rootPath, ...
        'IncludeSubfolders',true, ...
        'LabelSource','foldernames');
    files = lumenImageDataStore.Files;
    [pred, score] = classify(lumenNet, lumenImageDataStore);
    prediction = char(pred);
end


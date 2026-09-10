package com.ruoyi.vlstream.test.vlstream.data;

import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.vlstream.test.vlstream.enums.AlgorithmAnnotationTypeEnum;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import org.yaml.snakeyaml.LoaderOptions;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.function.LongConsumer;
import java.util.zip.*;
import static com.ruoyi.vlstream.test.vlstream.data.DataManagementService.map;

/** ZIP64-aware import pipeline whose memory usage does not grow with archive/file size. */
@Service
@RequiredArgsConstructor
public class DatasetArchiveImporter {
    private final DataManagementService data;
    private final DataMediaStorage storage;
    private final SampleMediaInspector inspector;
    private final DataTransferService transfer;
    @Value("${vlstream.data-management.max-unpacked-bytes:34359738368}") private long maxUnpackedBytes=32L*1024*1024*1024;

    public Map<String,Object> importFile(Long datasetId,Path file,String filename,String format,String sourceName,LongConsumer progress) throws IOException {
        if(!filename.toLowerCase(Locale.ROOT).endsWith(".zip")) {
            if(!"none".equals(format))throw new ServiceException("带标注导入请选择同时包含媒体和标注文件的 ZIP 归档");
            SampleMediaInspector.Inspection inspection=inspector.inspectFile(filename,file);
            String key="samples/"+data.tenant()+"/"+datasetId+"/"+UUID.randomUUID()+suffix(filename);
            storage.putFile(key,file,inspection.getContentType());
            try {
                Map<String,Object> result=data.register(datasetId,filename,key,Files.size(file),sourceName,inspection);
                if(Boolean.TRUE.equals(result.get("duplicate"))) storage.remove(key);
                result.put("filename",filename);result.put("success",true);
                return map("results",Collections.singletonList(result),"successCount",1);
            } catch(RuntimeException e) { try{storage.remove(key);}catch(RuntimeException ignored){} throw e; }
        }
        Path marker=transfer.tempFile("unpack-",".dir"); Files.delete(marker); Path directory=Files.createDirectory(marker);
        List<PendingSampleImport> staged=new ArrayList<>();
        try {
            Map<String,Path> entries=extract(file,directory,progress);
            Map<String,Path> media=new LinkedHashMap<>();
            entries.forEach((name,path)->{if(isMedia(name))media.put(name,path);});
            if(media.isEmpty()) throw new ServiceException("ZIP 内没有支持的图片或视频");
            if(media.size()>DataManagementService.MAX_SNAPSHOT_SAMPLES) throw new ServiceException("数据集最多 10000 个样本，请拆分数据集");
            DatasetSnapshot manifest=null;
            if("vls".equals(format)) {
                String manifestName=entries.keySet().stream().filter(k->k.equals("vls-samples.json")||k.endsWith("/vls-samples.json")).findFirst().orElseThrow(()->new ServiceException("未找到 vls-samples.json"));
                Path manifestFile=entries.get(manifestName);
                if(Files.size(manifestFile)>20*1024*1024) throw new ServiceException("标注清单超过 20 MB");
                manifest=data.readSnapshot(new String(Files.readAllBytes(manifestFile),StandardCharsets.UTF_8));
                String prefix=manifestName.substring(0,manifestName.length()-"vls-samples.json".length());
                for(AnnotationImage sample:manifest.getSamples()) sample.setLocalPath(prefix+sample.getLocalPath());
                transfer.validateManifest(datasetId,manifest,media);
            } else if("yolo".equals(format)) {
                manifest=yoloManifest(datasetId,media,entries);
            }
            Map<String,SampleMediaInspector.Inspection> inspections=new LinkedHashMap<>();
            long inspected=0;
            for(Map.Entry<String,Path> entry:media.entrySet()) {
                inspections.put(entry.getKey(),inspector.inspectFile(DataTransferService.safeName(entry.getKey()),entry.getValue()));
                progress.accept(++inspected);
            }
            if(manifest!=null) for(AnnotationImage sample:manifest.getSamples()) {
                SampleMediaInspector.Inspection inspection=inspections.get(sample.getLocalPath());
                if(inspection==null || (DataManagementService.hasText(sample.getContentSha256())&&!sample.getContentSha256().equals(inspection.getSha256())))
                    throw new ServiceException("标注清单文件缺失或校验和不符");
            }
            for(Map.Entry<String,Path> entry:media.entrySet()) {
                String name=DataTransferService.safeName(entry.getKey()); String key="samples/"+data.tenant()+"/"+datasetId+"/"+UUID.randomUUID()+suffix(name);
                SampleMediaInspector.Inspection inspection=inspections.get(entry.getKey());
                staged.add(new PendingSampleImport(entry.getKey(),name,key,Files.size(entry.getValue()),inspection));
                storage.putFile(key,entry.getValue(),inspection.getContentType()); progress.accept(++inspected);
            }
            Map<String,Object> result=data.registerArchiveFromSource(datasetId,staged,manifest,sourceName);
            List<?> rows=(List<?>)result.get("results");
            for(int i=0;i<staged.size();i++) if(Boolean.TRUE.equals(((Map<?,?>)rows.get(i)).get("duplicate"))) cleanup(staged.get(i).getObjectKey());
            return result;
        } catch(IOException|RuntimeException failure) {
            staged.forEach(item->cleanup(item.getObjectKey())); throw failure;
        } finally {
            try(java.util.stream.Stream<Path> paths=Files.walk(directory)) {
                for(Path path:(Iterable<Path>)paths.sorted(Comparator.reverseOrder())::iterator) Files.deleteIfExists(path);
            }
        }
    }

    Map<String,Path> extract(Path archive,Path directory,LongConsumer progress) throws IOException {
        Map<String,Path> entries=new LinkedHashMap<>(); long total=0;
        try(ZipInputStream zip=new ZipInputStream(new BufferedInputStream(Files.newInputStream(archive)))) {
            ZipEntry entry;
            while((entry=zip.getNextEntry())!=null) {
                String name=entry.getName().replace('\\','/'); Path target=directory.resolve(name).normalize();
                if(!target.startsWith(directory)||name.startsWith("/")||name.contains(":")||name.contains("../")||entries.containsKey(name)) throw new ServiceException("ZIP 含非法或重复路径");
                if(entry.isDirectory()) continue;
                if(entries.size()>=30000) throw new ServiceException("ZIP 文件数量超过 30000");
                Files.createDirectories(target.getParent());
                try(OutputStream output=Files.newOutputStream(target,StandardOpenOption.CREATE_NEW)) {
                    byte[] buffer=new byte[128*1024]; int count; long sinceCheck=0;
                    while((count=zip.read(buffer))!=-1) {
                        total+=count; sinceCheck+=count;
                        if(total>maxUnpackedBytes) throw new ServiceException("ZIP 解压总量超过当前容量限额");
                        if(sinceCheck>=32L*1024*1024) {
                            if(Files.getFileStore(directory).getUsableSpace()<512L*1024*1024) throw new ServiceException("暂存磁盘空间不足，已停止解压");
                            progress.accept(total);sinceCheck=0;
                        }
                        output.write(buffer,0,count);
                    }
                }
                entries.put(name,target);progress.accept(total);
            }
        }
        return entries;
    }

    private DatasetSnapshot yoloManifest(Long datasetId,Map<String,Path> media,Map<String,Path> entries) throws IOException {
        if(!"object_detection".equals(data.project(datasetId).getAnnotationType())) throw new ServiceException("YOLO 检测框归档仅能导入物体检测数据集");
        Path yamlFile=entries.entrySet().stream().filter(e->e.getKey().matches("(?i).*(data|dataset)\\.ya?ml$")).map(Map.Entry::getValue).findFirst().orElse(null);
        if(yamlFile==null || Files.size(yamlFile)>1024*1024) throw new ServiceException("YOLO ZIP 需要 data.yaml 或 dataset.yaml 类别定义");
        LoaderOptions options=new LoaderOptions(); options.setAllowDuplicateKeys(false); options.setMaxAliasesForCollections(20);
        Object config;
        try(InputStream input=Files.newInputStream(yamlFile)) { config=new Yaml(new SafeConstructor(options)).load(input); }
        if(!(config instanceof Map)) throw new ServiceException("无效的 YOLO 类别定义");
        Object names=((Map<?,?>)config).get("names"); List<AnnotationLabel> labels=new ArrayList<>();
        if(names instanceof List) { for(int i=0;i<((List<?>)names).size();i++) labels.add(label(i,((List<?>)names).get(i))); }
        else if(names instanceof Map) { for(Map.Entry<?,?> entry:((Map<?,?>)names).entrySet()) labels.add(label(Integer.parseInt(entry.getKey().toString()),entry.getValue())); }
        else throw new ServiceException("YOLO YAML 缺少 names");
        Set<Long> labelIds=new HashSet<>(); labels.forEach(l->labelIds.add(l.getId()));
        DatasetSnapshot manifest=new DatasetSnapshot();manifest.setAnnotationType("object_detection");manifest.setLabels(labels);
        manifest.setSamples(new ArrayList<>());manifest.setInstances(new ArrayList<>());
        long imageId=1;
        for(Map.Entry<String,Path> entry:media.entrySet()) {
            String name=entry.getKey();AnnotationImage sample=new AnnotationImage();sample.setId(imageId++);sample.setImageName(DataTransferService.safeName(name));sample.setLocalPath(name);sample.setSampleTags("[]");manifest.getSamples().add(sample);
            String stem=name.substring(0,name.lastIndexOf('.'));
            Path labelFile=entries.get(stem.replaceFirst("(^|/)images/","$1labels/")+".txt");
            if(labelFile==null)labelFile=entries.get(stem+".txt");
            if(labelFile==null) continue;
            if(Files.size(labelFile)>1024*1024) throw new ServiceException("单图片标注文件过大");
            SampleMediaInspector.Inspection dimensions=inspector.inspectFile(sample.getImageName(),entry.getValue());
            for(String line:Files.readAllLines(labelFile,StandardCharsets.UTF_8)) {
                if(line.trim().isEmpty())continue; String[] fields=line.trim().split("\\s+");
                if(fields.length!=5)throw new ServiceException("暂仅支持 YOLO 检测框：类别 cx cy w h");
                long id=Long.parseLong(fields[0])+1;double cx=Double.parseDouble(fields[1]),cy=Double.parseDouble(fields[2]),w=Double.parseDouble(fields[3]),h=Double.parseDouble(fields[4]);
                if(!labelIds.contains(id)||!Double.isFinite(cx+cy+w+h)||w<=0||h<=0||cx-w/2< -0.000001||cy-h/2< -0.000001||cx+w/2>1.000001||cy+h/2>1.000001)throw new ServiceException("YOLO 类别或归一化坐标无效");
                AnnotationInstance instance=new AnnotationInstance();instance.setImageId(sample.getId());instance.setLabelId(id);instance.setAnnotationType(AlgorithmAnnotationTypeEnum.rect);
                instance.setAnnotationData(data.writeJson(map("x",Math.max(0,cx-w/2)*dimensions.getWidth(),"y",Math.max(0,cy-h/2)*dimensions.getHeight(),"width",w*dimensions.getWidth(),"height",h*dimensions.getHeight())));
                manifest.getInstances().add(instance);
                if(manifest.getInstances().size()>100000)throw new ServiceException("标注数量超过 100000");
            }
        }
        return manifest;
    }
    private AnnotationLabel label(int index,Object name) {
        if(index<0||index>999||name==null||name.toString().trim().isEmpty()||name.toString().length()>50)throw new ServiceException("YOLO 类别名称或编号无效");
        AnnotationLabel label=new AnnotationLabel();label.setId(index+1L);label.setName(name.toString());label.setColor("#409eff");return label;
    }
    private void cleanup(String key) {try{storage.remove(key);}catch(RuntimeException ignored){} }
    static boolean isMedia(String name) {return name.matches("(?i).+\\.(jpg|jpeg|png|bmp|mp4|mov)$");}
    private String suffix(String name){return name.substring(name.lastIndexOf('.')).toLowerCase(Locale.ROOT);}
}

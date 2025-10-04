package com.smartLive.file.controller;

import com.smartLive.common.core.web.domain.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.smartLive.common.core.domain.R;
import com.smartLive.common.core.utils.StringUtils;
import com.smartLive.common.core.utils.file.FileUtils;
import com.smartLive.file.service.ISysFileService;
import com.smartLive.system.api.domain.SysFile;

/**
 * 文件请求处理
 * 
 * @author smartLive
 */
@RestController
public class SysFileController
{
    private static final Logger log = LoggerFactory.getLogger(SysFileController.class);

    //使用minio上传文件
    @Autowired
    @Qualifier("minioSysFileServiceImpl")
    private ISysFileService minioSysFileServiceImpl;

    /**
     * 文件上传请求
     */
    @PostMapping("/upload")
    public R<SysFile> upload(MultipartFile file)
    {
        try
        {
            // 上传并返回访问地址
            String url = minioSysFileServiceImpl.uploadFile(file);
            SysFile sysFile = new SysFile();
            sysFile.setName(FileUtils.getName(url));
            sysFile.setUrl(url);
            return R.ok(sysFile);
        }
        catch (Exception e)
        {
            log.error("上传文件失败", e);
            return R.fail(e.getMessage());
        }
    }

    /**
     * 文件删除请求
     */
    @DeleteMapping("delete")
    public R<Boolean> delete(String fileUrl)
    {
        try
        {
            if (!FileUtils.validateFilePath(fileUrl))
            {
                throw new Exception(StringUtils.format("资源文件({})非法，不允许删除。 ", fileUrl));
            }
            minioSysFileServiceImpl.deleteFile(fileUrl);
            return R.ok();
        }
        catch (Exception e)
        {
            log.error("删除文件失败", e);
            return R.fail(e.getMessage());
        }
    }
    /**
     * app端文件上传请求
     */
    @PostMapping("/appUpload")
    public Result appUpload(MultipartFile file)
    {
        try
        {
            // 上传并返回访问地址
            String url = minioSysFileServiceImpl.uploadFile(file);
            SysFile sysFile = new SysFile();
            sysFile.setName(FileUtils.getName(url));
            sysFile.setUrl(url);
            return Result.ok(sysFile.getUrl());
        }
        catch (Exception e)
        {
            log.error("上传文件失败", e);
            return Result.fail(e.getMessage());
        }
    }
    /**
     * 文件删除请求
     */
    @GetMapping("/appDelete")
    public Result appDelete(@RequestParam("name") String fileUrl)
    {
        try
        {
            if (!FileUtils.validateFilePath(fileUrl))
            {
                throw new Exception(StringUtils.format("资源文件({})非法，不允许删除。 ", fileUrl));
            }
            minioSysFileServiceImpl.deleteFile(fileUrl);
            return Result.ok();
        }
        catch (Exception e)
        {
            log.error("删除文件失败", e);
            return Result.fail(e.getMessage());
        }
    }
}

package com.xiaozhi.knowledge;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.stp.StpUtil;
import com.xiaozhi.ai.knowledge.KnowledgeIndexer;
import com.xiaozhi.ai.knowledge.KnowledgeRetriever;
import com.xiaozhi.common.annotation.AuditLog;
import com.xiaozhi.common.exception.OperationFailedException;
import com.xiaozhi.common.model.PageResult;
import com.xiaozhi.common.model.bo.KnowledgeBaseBO;
import com.xiaozhi.common.model.bo.KnowledgeChunkBO;
import com.xiaozhi.common.model.bo.KnowledgeDocBO;
import com.xiaozhi.common.model.req.KnowledgeBasePageReq;
import com.xiaozhi.common.model.req.KnowledgeBaseSaveReq;
import com.xiaozhi.common.model.req.KnowledgeSearchReq;
import com.xiaozhi.common.model.resp.KnowledgeBaseResp;
import com.xiaozhi.common.model.resp.KnowledgeDocResp;
import com.xiaozhi.common.model.resp.KnowledgeHitResp;
import com.xiaozhi.common.web.ApiResponse;
import com.xiaozhi.knowledge.convert.KnowledgeConvert;
import com.xiaozhi.knowledge.service.KnowledgeService;
import com.xiaozhi.server.web.BaseController;
import com.xiaozhi.storage.service.StorageServiceFactory;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

/**
 * 知识库管理：库的增删改查、文档上传与索引、检索测试。
 */
@RestController
@RequestMapping("/api/knowledge")
@Tag(name = "知识库", description = "文档入库与检索")
public class KnowledgeController extends BaseController {

    @Resource
    private KnowledgeService knowledgeService;

    @Resource
    private KnowledgeConvert knowledgeConvert;

    @Resource
    private KnowledgeIndexer knowledgeIndexer;

    @Resource
    private KnowledgeRetriever knowledgeRetriever;

    @Resource
    private StorageServiceFactory storageServiceFactory;

    @GetMapping("")
    @ResponseBody
    @SaCheckPermission("system:knowledge:api:list")
    @Operation(summary = "知识库列表", description = "按名称分页查询")
    public ApiResponse<PageResult<KnowledgeBaseResp>> list(@Valid KnowledgeBasePageReq req) {
        return ApiResponse.success(knowledgeService
            .page(req.getPageNo(), req.getPageSize(), req.getName())
            .map(knowledgeConvert::toResp));
    }

    @PostMapping("")
    @ResponseBody
    @SaCheckPermission("system:knowledge:api:create")
    @AuditLog(module = "知识库", operation = "创建知识库")
    @Operation(summary = "创建知识库", description = "可绑定角色与向量模型配置")
    public ApiResponse<KnowledgeBaseResp> create(@Valid @RequestBody KnowledgeBaseSaveReq req) {
        KnowledgeBaseBO bo = knowledgeConvert.toBO(req);
        bo.setCreateUserId(StpUtil.getLoginIdAsInt());
        return ApiResponse.success(knowledgeConvert.toResp(knowledgeService.create(bo)));
    }

    @PutMapping("/{knowledgeId}")
    @ResponseBody
    @SaCheckPermission("system:knowledge:api:update")
    @AuditLog(module = "知识库", operation = "更新知识库")
    @Operation(summary = "更新知识库", description = "修改名称、说明、绑定角色或向量模型")
    public ApiResponse<KnowledgeBaseResp> update(@PathVariable Integer knowledgeId,
                                                 @Valid @RequestBody KnowledgeBaseSaveReq req) {
        KnowledgeBaseBO bo = knowledgeConvert.toBO(req);
        bo.setRoleId(req.getRoleId());
        bo.setEmbeddingConfigId(req.getEmbeddingConfigId());
        bo.setState(req.getState());
        return ApiResponse.success(knowledgeConvert.toResp(knowledgeService.update(knowledgeId, bo)));
    }

    @DeleteMapping("/{knowledgeId}")
    @ResponseBody
    @SaCheckPermission("system:knowledge:api:delete")
    @AuditLog(module = "知识库", operation = "删除知识库")
    @Operation(summary = "删除知识库", description = "库里还有文档时不允许删除")
    public ApiResponse<Void> delete(@PathVariable Integer knowledgeId) {
        knowledgeService.delete(knowledgeId);
        return ApiResponse.success("删除成功");
    }

    @GetMapping("/{knowledgeId}/doc")
    @ResponseBody
    @SaCheckPermission("system:knowledge:api:list")
    @Operation(summary = "文档列表", description = "返回该知识库下的全部文档")
    public ApiResponse<List<KnowledgeDocResp>> docs(@PathVariable Integer knowledgeId) {
        return ApiResponse.success(knowledgeService.listDocs(knowledgeId).stream()
            .map(knowledgeConvert::toDocResp)
            .toList());
    }

    @PostMapping("/{knowledgeId}/doc")
    @ResponseBody
    @SaCheckPermission("system:knowledge:doc:api:upload")
    @AuditLog(module = "知识库", operation = "上传文档")
    @Operation(summary = "上传文档", description = "上传后立即解析、切片并向量化")
    public ApiResponse<KnowledgeDocResp> upload(@PathVariable Integer knowledgeId,
                                                @RequestParam("file") MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new OperationFailedException("文件为空");
        }
        KnowledgeBaseBO base = knowledgeService.getBO(knowledgeId);
        String fileName = StringUtils.hasText(file.getOriginalFilename()) ? file.getOriginalFilename() : "untitled";
        // MultipartFile 版本要分别给目录与文件名：存储层据此决定落盘路径
        String relativePath = "knowledge/" + knowledgeId;
        String uniqueName = System.nanoTime() + "-" + fileName;
        String storedPath = storageServiceFactory.getStorageService().upload(file, relativePath, uniqueName);

        KnowledgeDocBO doc = new KnowledgeDocBO();
        doc.setKnowledgeId(knowledgeId);
        doc.setFileName(fileName);
        doc.setStoredPath(storedPath);
        doc.setFileType(extensionOf(fileName));
        doc.setFileSize(file.getSize());
        doc.setCreateUserId(StpUtil.getLoginIdAsInt());
        KnowledgeDocBO saved = knowledgeService.createDoc(doc);

        knowledgeIndexer.index(saved.getDocId(), base);
        return ApiResponse.success(knowledgeService.listDocs(knowledgeId).stream()
            .filter(item -> saved.getDocId().equals(item.getDocId()))
            .findFirst()
            .map(knowledgeConvert::toDocResp)
            .orElseGet(() -> knowledgeConvert.toDocResp(saved)));
    }

    @DeleteMapping("/doc/{docId}")
    @ResponseBody
    @SaCheckPermission("system:knowledge:doc:api:delete")
    @AuditLog(module = "知识库", operation = "删除文档")
    @Operation(summary = "删除文档", description = "同时删除该文档的全部分片")
    public ApiResponse<Void> deleteDoc(@PathVariable Long docId) {
        knowledgeService.deleteDoc(docId);
        return ApiResponse.success("删除成功");
    }

    @PostMapping("/search")
    @ResponseBody
    @SaCheckPermission("system:knowledge:api:search")
    @Operation(summary = "检索测试", description = "验证召回效果，返回命中分片与相似度")
    public ApiResponse<List<KnowledgeHitResp>> search(@Valid @RequestBody KnowledgeSearchReq req) {
        List<KnowledgeBaseBO> bases = req.getKnowledgeId() == null
            ? knowledgeService.listEnabled()
            : List.of(knowledgeService.getBO(req.getKnowledgeId()));
        int topK = req.getTopK() == null ? 5 : Math.min(req.getTopK(), 20);
        List<KnowledgeChunkBO> hits = knowledgeRetriever.search(bases, req.getQuery(), topK);
        List<KnowledgeHitResp> resp = hits.stream().map(hit -> {
            KnowledgeHitResp item = new KnowledgeHitResp();
            item.setChunkId(hit.getChunkId());
            item.setDocId(hit.getDocId());
            item.setKnowledgeId(hit.getKnowledgeId());
            item.setContent(hit.getContent());
            return item;
        }).toList();
        return ApiResponse.success(resp);
    }

    private String extensionOf(String fileName) {
        int index = fileName.lastIndexOf('.');
        return index > 0 ? fileName.substring(index + 1).toLowerCase() : null;
    }
}

-- 曲库管理页：列表 / 删除权限
INSERT IGNORE INTO `sys_permission` (`permissionId`,`parentId`,`name`,`permissionKey`,`permissionType`,`path`,`component`,`icon`,`sort`,`visible`,`status`,`createTime`,`updateTime`)
VALUES (210,17,'曲库列表','system:file:api:list','api',NULL,NULL,NULL,3,'1','1',NOW(),NOW()),
       (211,17,'删除曲库文件','system:file:api:delete','api',NULL,NULL,NULL,4,'1','1',NOW(),NOW());

INSERT IGNORE INTO `sys_auth_role_permission` (`authRoleId`,`permissionId`) VALUES (1,210),(1,211);

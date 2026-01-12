// 全局状态
let currentBucket = '';
let currentPrefix = '';
let selectedFile = null;
let buckets = [];
let policyTemplates = [];
let selectedPolicyType = '';
let selectedTemplatePolicyType = '';
let editingTemplate = null;
let currentTheme = 'light';

// Tab 切换
document.querySelectorAll('.tab').forEach(tab => {
    tab.addEventListener('click', () => {
        document.querySelectorAll('.tab').forEach(t => t.classList.remove('active'));
        document.querySelectorAll('.tab-content').forEach(c => c.classList.remove('active'));
        tab.classList.add('active');
        document.getElementById(tab.dataset.tab + '-tab').classList.add('active');
    });
});

// 主题切换函数
function toggleTheme() {
    currentTheme = currentTheme === 'light' ? 'dark' : 'light';
    applyTheme(currentTheme);
    localStorage.setItem('theme', currentTheme);
}

function applyTheme(theme) {
    const themeIcon = document.getElementById('themeIcon');
    if (theme === 'dark') {
        document.documentElement.setAttribute('data-theme', 'dark');
        themeIcon.textContent = '☀️';
    } else {
        document.documentElement.removeAttribute('data-theme');
        themeIcon.textContent = '🌙';
    }
}

function initTheme() {
    // 从 localStorage 读取主题设置
    const savedTheme = localStorage.getItem('theme');
    // 检查系统偏好
    const prefersDark = window.matchMedia('(prefers-color-scheme: dark)').matches;

    currentTheme = savedTheme || (prefersDark ? 'dark' : 'light');
    applyTheme(currentTheme);
}

// 页面加载时初始化
window.onload = function() {
    initTheme();
    refreshBuckets();
    refreshPolicyTemplates();

    // 从 URL 参数获取 bucket
    const urlParams = new URLSearchParams(window.location.search);
    const bucketParam = urlParams.get('bucket');
    if (bucketParam) {
        setTimeout(() => openBucketFiles(bucketParam), 500);
    }
};

// ==================== 存储桶管理 ====================
async function refreshBuckets() {
    const btn = document.getElementById('refreshBucketsBtn');
    btn.disabled = true;
    btn.innerHTML = '<span class="loading"></span> 刷新中...';

    try {
        const response = await fetch('/oss/listBuckets');
        const result = await response.json();
        if (result.code === '0000' && result.data) {
            buckets = result.data;
            displayBuckets(buckets);
            updateBucketSelects(buckets);
        } else {
            alert('查询失败: ' + (result.message || '未知错误'));
        }
    } catch (error) {
        alert('网络错误: ' + error.message);
    } finally {
        btn.disabled = false;
        btn.innerHTML = '<span>🔄</span> 刷新';
    }
}

function displayBuckets(bucketList) {
    const container = document.getElementById('bucketList');
    if (bucketList.length === 0) {
        container.innerHTML = `
            <div class="empty-state">
                <div class="empty-state-icon">🗄️</div>
                暂无存储桶，点击"新建存储桶"创建
            </div>
        `;
        return;
    }
    container.innerHTML = bucketList.map(bucket => `
        <div class="bucket-card" onclick="openBucketFiles('${bucket}')">
            <div class="bucket-name">🗄️ ${bucket}</div>
            <div class="bucket-actions">
                <button class="btn btn-secondary btn-small" onclick="event.stopPropagation(); viewBucketPolicy('${bucket}')">📋 策略</button>
                <button class="btn btn-danger btn-small" onclick="event.stopPropagation(); confirmDeleteBucket('${bucket}')">🗑️ 删除</button>
            </div>
        </div>
    `).join('');
}

function updateBucketSelects(bucketList) {
    const selects = ['newBucketPolicyTemplate', 'customPolicyTemplate'];
    selects.forEach(selectId => {
        const select = document.getElementById(selectId);
        if (select) {
            const options = '<option value="">选择策略模板...</option>' +
                policyTemplates.map(t => `<option value="${t.templateName}">${t.templateName}</option>`).join('');
            select.innerHTML = options;
        }
    });
}

function showCreateBucketModal() {
    document.getElementById('createBucketModal').classList.add('show');
    document.getElementById('newBucketName').value = '';
    updateBucketSelects(buckets);
}

async function confirmCreateBucket() {
    const bucketName = document.getElementById('newBucketName').value.trim();
    const templateName = document.getElementById('newBucketPolicyTemplate').value;

    if (!bucketName) {
        alert('请输入存储桶名称');
        return;
    }

    if (templateName) {
        // 使用策略模板创建存储桶
        const template = policyTemplates.find(t => t.templateName === templateName);
        if (template) {
            const policy = template.policy.replace(/{bucket}/g, bucketName);
            const bucketVO = {
                bucketName: bucketName,
                bucketPolicyList: [policy]
            };

            try {
                const response = await fetch('/oss/createCustomBucket', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify(bucketVO)
                });
                const result = await response.json();
                if (result.code === '0000') {
                    alert('✅ 创建成功');
                    closeModal('createBucketModal');
                    refreshBuckets();
                } else {
                    alert('❌ 创建失败: ' + (result.message || '未知错误'));
                }
            } catch (error) {
                alert('网络错误: ' + error.message);
            }
            return;
        }
    }

    // 默认创建存储桶
    try {
        const response = await fetch(`/oss/createBucket?bucketName=${encodeURIComponent(bucketName)}`);
        const result = await response.json();
        if (result.code === '0000') {
            alert('✅ 创建成功');
            closeModal('createBucketModal');
            refreshBuckets();
        } else {
            alert('❌ 创建失败: ' + (result.message || '未知错误'));
        }
    } catch (error) {
        alert('网络错误: ' + error.message);
    }
}

function confirmDeleteBucket(bucketName) {
    if (!confirm(`⚠️ 确定要删除存储桶 "${bucketName}" 吗？\n\n⚠️ 注意：存储桶必须为空才能删除！`)) {
        return;
    }
    deleteBucketByName(bucketName);
}

async function deleteBucketByName(bucketName) {
    try {
        const response = await fetch(`/oss/deleteBucket/${encodeURIComponent(bucketName)}`, { method: 'DELETE' });
        if (response.ok) {
            const result = await response.json();
            if (result.code === '0000') {
                alert('✅ 删除成功');
                refreshBuckets();
            } else {
                alert('❌ 删除失败: ' + (result.message || '未知错误'));
            }
        } else {
            alert('❌ 删除失败');
        }
    } catch (error) {
        alert('网络错误: ' + error.message);
    }
}

// ==================== 存储桶文件管理 ====================
function openBucketFiles(bucketName) {
    currentBucket = bucketName;
    currentPrefix = '';

    document.getElementById('currentBucketName').textContent = bucketName;

    // 获取并显示存储桶策略类型
    fetchBucketPolicyType(bucketName);

    document.querySelectorAll('.tab').forEach(t => t.classList.remove('active'));
    document.querySelectorAll('.tab-content').forEach(c => c.classList.remove('active'));
    document.getElementById('buckets-tab').classList.add('active');
    document.getElementById('bucketFiles-tab').classList.add('active');

    refreshFiles();
}

/**
 * 获取并显示存储桶策略类型
 */
async function fetchBucketPolicyType(bucketName) {
    const policyBadge = document.getElementById('currentBucketPolicyType');
    policyBadge.className = 'policy-badge';
    policyBadge.textContent = '加载中...';

    try {
        const response = await fetch(`/oss/getBucketPolicy?bucketName=${encodeURIComponent(bucketName)}`);
        const result = await response.json();
        
        if (result.code === '0000' && result.data) {
            const policyType = determinePolicyType(result.data);
            policyBadge.className = `policy-badge ${policyType}`;
            policyBadge.textContent = getPolicyTypeLabel(policyType);
        } else {
            policyBadge.className = 'policy-badge custom';
            policyBadge.textContent = '未知';
        }
    } catch (error) {
        console.error('获取存储桶策略失败', error);
        policyBadge.className = 'policy-badge private';
        policyBadge.textContent = '未知';
    }
}

function getPolicyTypeLabel(policyType) {
    switch(policyType) {
        case 'public': return '公有';
        case 'readonly': return '只读';
        case 'private': return '私有';
        case 'custom': return '自定义';
        default: return policyType;
    }
}

/**
 * 根据策略 JSON 判断策略类型
 */
function determinePolicyType(policyJson) {
    if (!policyJson || policyJson.trim() === '') {
        return 'private';
    }

    try {
        const policy = JSON.parse(policyJson);

        if (!policy.Statement || policy.Statement.length === 0) {
            return 'private';
        }

        // 收集所有 Statement 的 actions
        let allActions = [];
        let hasPublicPrincipal = false;

        for (const statement of policy.Statement) {
            const actions = statement.Action || [];
            const principal = statement.Principal;

            // 检查 Principal 是否是 *
            if (principal && principal.AWS && principal.AWS.includes('*')) {
                hasPublicPrincipal = true;
            }

            // 收集所有 actions
            allActions = allActions.concat(actions);
        }

        if (!hasPublicPrincipal) {
            return 'custom';
        }

        // 去重
        const uniqueActions = [...new Set(allActions)];

        console.log('策略类型判断 - Actions:', uniqueActions);

        // 检查权限类型
        const hasGetObject = uniqueActions.includes('s3:GetObject');
        const hasPutObject = uniqueActions.includes('s3:PutObject');
        const hasDeleteObject = uniqueActions.includes('s3:DeleteObject');
        const hasListOperations = uniqueActions.some(action =>
            action.includes('s3:ListBucket') ||
            action.includes('s3:ListAllMyBuckets') ||
            action.includes('s3:ListBucketMultipartUploads')
        );

        // 私有策略：没有公开的 principal
        if (!hasPublicPrincipal) {
            return 'private';
        }

        // 公有策略：包含完整的读写权限（GetObject + DeleteObject + ListOperation）
        if (hasGetObject && hasDeleteObject && hasListOperations) {
            return 'public';
        }

        // 只读策略：只有 GetObject，没有其他操作
        if (hasGetObject && !hasPutObject && !hasDeleteObject && !hasListOperations) {
            return 'readonly';
        }

        // 自定义策略：其他所有情况
        return 'custom';

    } catch (e) {
        console.error('解析策略失败', e);
        return 'custom';
    }
}

function backToBuckets() {
    document.querySelectorAll('.tab').forEach(t => t.classList.remove('active'));
    document.querySelectorAll('.tab-content').forEach(c => c.classList.remove('active'));
    document.querySelector('[data-tab="buckets"]').classList.add('active');
    document.getElementById('buckets-tab').classList.add('active');
}

async function refreshFiles() {
    const btn = document.getElementById('refreshFilesBtn');
    btn.disabled = true;
    btn.innerHTML = '<span class="loading"></span> 刷新中...';

    try {
        const response = await fetch(`/oss/listFilesByBucketName?bucketName=${encodeURIComponent(currentBucket)}&prefix=${encodeURIComponent(currentPrefix)}&size=100`);
        const result = await response.json();
        if (result.code === '0000') {
            displayFiles(result.data || []);
            updateBreadcrumb(currentPrefix);
        } else {
            alert('查询失败: ' + (result.message || '未知错误'));
        }
    } catch (error) {
        alert('网络错误: ' + error.message);
    } finally {
        btn.disabled = false;
        btn.innerHTML = '<span>🔄</span> 刷新';
    }
}

function displayFiles(files) {
    const container = document.getElementById('fileList');
    if (!files || files.length === 0) {
        container.innerHTML = `
            <div class="empty-state">
                <div class="empty-state-icon">📁</div>
                当前目录为空
            </div>
        `;
        return;
    }

    console.log('========== displayFiles ==========');
    console.log('当前前缀:', JSON.stringify(currentPrefix));
    console.log('后端返回的文件数量:', files.length);
    console.log('文件列表:', files);

    // 后端返回相对路径，直接使用
    const items = files.map(file => {
        // 后端返回的已经是相对路径（如 2025/ 或 2025/12/）
        const path = file.trim();
        const isFolder = file.endsWith('/') || path.endsWith('/');

        // 提取最后一部分作为显示名称
        const parts = path.split('/').filter(p => p);
        const name = parts[parts.length - 1] || '';

        console.log(`处理文件: ${JSON.stringify(file)} -> 路径: ${JSON.stringify(path)}, 名称: ${JSON.stringify(name)}, 文件夹: ${isFolder}`);

        return {
            name: name,
            isFolder: isFolder,
            fullPath: path  // 保留完整路径
        };
    });

    // 排序（文件夹在前，文件在后）
    items.sort((a, b) => {
        if (a.isFolder !== b.isFolder) {
            return a.isFolder ? -1 : 1;
        }
        return a.name.localeCompare(b.name);
    });

    console.log('处理后的项目:', items);

    container.innerHTML = items.map(item => `
        <div class="file-item ${item.isFolder ? 'folder-item' : ''}">
            <div class="file-info" onclick="${item.isFolder ? `enterFolder('${item.fullPath}')` : `openFile('${item.fullPath}')`}">
                <div class="file-name">
                    <span class="file-icon">${item.isFolder ? '📁' : '📄'}</span>
                    <span>${item.name}</span>
                </div>
            </div>
            <div class="file-actions">
                ${!item.isFolder ? `
                    <button class="btn btn-primary btn-small" onclick="event.stopPropagation(); openFile('${item.fullPath}')">⬇️ 打开</button>
                    <button class="btn btn-secondary btn-small" onclick="event.stopPropagation(); quickGeneratePresignedUrl('${item.fullPath}')">🔗 临时链接</button>
                    <button class="btn btn-danger btn-small" onclick="event.stopPropagation(); confirmDeleteFile('${item.fullPath}')">🗑️ 删除</button>
                ` : `
                    <button class="btn btn-primary btn-small" onclick="event.stopPropagation(); enterFolder('${item.fullPath}')">进入</button>
                    `}
            </div>
        </div>
    `).join('');
}

// 提取相对路径（移除 CDN 前缀）
function getRelativePath(fullPath) {
    if (!fullPath) return '';
    console.log('getRelativePath 输入:', JSON.stringify(fullPath));
    // 移除 http:// 或 https:// 前缀
    const result = fullPath.replace(/^https?:\/\/[^\/]+/, '');
    console.log('getRelativePath 输出:', JSON.stringify(result));
    return result;
}

function enterFolder(folderPath) {
    console.log('========== enterFolder 被调用 ==========');
    console.log('输入路径:', JSON.stringify(folderPath));
    console.log('当前前缀:', JSON.stringify(currentPrefix));

    // 检查 folderPath 是否已经是完整路径（包含 currentPrefix）
    let newPath;
    if (currentPrefix && folderPath.startsWith(currentPrefix)) {
        // 如果 folderPath 已经包含 currentPrefix，直接使用
        newPath = folderPath;
    } else {
        // 否则，从 folderPath 中提取相对路径（移除 http 前缀）
        newPath = getRelativePath(folderPath);
    }

    console.log('处理后路径:', JSON.stringify(newPath));
    currentPrefix = newPath;
    console.log('设置 currentPrefix 为:', JSON.stringify(currentPrefix));

    refreshFiles();
}

function navigateToRoot() {
    currentPrefix = '';
    refreshFiles();
}

function updateBreadcrumb(prefix) {
    const breadcrumb = document.getElementById('breadcrumb');
    if (!prefix) {
        breadcrumb.style.display = 'none';
        return;
    }

    console.log('========== 更新面包屑 ==========');
    console.log('输入前缀:', JSON.stringify(prefix));

    const parts = prefix.split('/').filter(p => p);
    console.log('分割后的部分:', parts);

    let html = `<span class="breadcrumb-item" onclick="navigateToRoot()">🏠 根目录</span>`;

    let currentPath = '';
    parts.forEach((part, index) => {
        currentPath += part + '/';
        console.log(`部分 ${index}: ${part}, 累积路径: ${JSON.stringify(currentPath)}`);

        html += `<span class="breadcrumb-separator">›</span>`;
        if (index === parts.length - 1) {
            html += `<span class="breadcrumb-current">${part}</span>`;
        } else {
            html += `<span class="breadcrumb-item" onclick="enterFolder('${currentPath}')">${part}</span>`;
        }
    });

    console.log('最终面包屑HTML:', html);
    breadcrumb.innerHTML = html;
    breadcrumb.style.display = 'flex';
}

function openFile(filePath) {
    // 拼接完整 URL 打开文件
    const fullUrl = filePath.startsWith('http') ? filePath : `https://oss.infoq.cc/${currentBucket}/${filePath}`;
    window.open(fullUrl, '_blank');
}

async function confirmDeleteFile(filePath) {
    if (!confirm(`⚠️ 确定要删除此文件吗？\n\n${filePath}`)) {
        return;
    }

    try {
        const response = await fetch(`/oss/removeFile/${encodeURIComponent(currentBucket)}?filePath=${encodeURIComponent(filePath)}`, { method: 'DELETE' });
        if (response.ok) {
            alert('✅ 删除成功');
            refreshFiles();
        } else {
            alert('❌ 删除失败');
        }
    } catch (error) {
        alert('网络错误: ' + error.message);
    }
}

async function deleteBucket() {
    confirmDeleteBucket(currentBucket);
}

// ==================== 临时访问链接 ====================
async function showPresignedUrlModal() {
    document.getElementById('presignedFilePath').value = currentPrefix || '';
    document.getElementById('presignedUrlResult').value = '';
    document.getElementById('presignedUrlModal').classList.add('show');
}

async function quickGeneratePresignedUrl(filePath) {
    try {
        const response = await fetch(`/oss/getPresignedObjectUrl?bucketName=${encodeURIComponent(currentBucket)}&objectName=${encodeURIComponent(filePath)}&expirySeconds=3600`);
        const result = await response.json();

        if (result.code === '0000') {
            // 自动复制到剪贴板
            navigator.clipboard.writeText(result.data).then(() => {
                alert('✓ 临时链接已生成并复制到剪贴板（有效期 1 小时）');
            }).catch(err => {
                // 如果复制失败，显示链接
                alert(`✓ 临时链接已生成：\n${result.data}`);
            });
        } else {
            alert('生成失败: ' + (result.message || '未知错误'));
        }
    } catch (error) {
        alert('网络错误: ' + error.message);
    }
}

async function generatePresignedUrl() {
    const filePath = document.getElementById('presignedFilePath').value.trim();
    const expiry = document.getElementById('presignedExpiry').value;

    if (!filePath) {
        alert('请输入文件路径');
        return;
    }

    if (!expiry || expiry < 60 || expiry > 604800) {
        alert('过期时间必须在 60 秒到 604800 秒（7天）之间');
        return;
    }

    try {
        const response = await fetch(`/oss/getPresignedObjectUrl?bucketName=${encodeURIComponent(currentBucket)}&objectName=${encodeURIComponent(filePath)}&expirySeconds=${expiry}`);
        const result = await response.json();

        if (result.code === '0000') {
            document.getElementById('presignedUrlResult').value = result.data;
        } else {
            alert('生成失败: ' + (result.message || '未知错误'));
        }
    } catch (error) {
        alert('网络错误: ' + error.message);
    }
}

function copyPresignedUrl() {
    const url = document.getElementById('presignedUrlResult').value;

    if (!url) {
        alert('请先生成访问链接');
        return;
    }

    navigator.clipboard.writeText(url).then(() => {
        alert('✓ 链接已复制到剪贴板');
    }).catch(err => {
        alert('复制失败: ' + err.message);
    });
}

// ==================== 存储桶策略 ====================
async function viewBucketPolicy(bucketName) {
    currentBucket = bucketName;
    showBucketPolicyModal();
}

async function showBucketPolicyModal() {
    document.getElementById('bucketPolicyModal').classList.add('show');
    document.getElementById('bucketPolicyContent').value = '';

    try {
        const response = await fetch(`/oss/getBucketPolicy?bucketName=${encodeURIComponent(currentBucket)}`);
        const result = await response.json();
        if (result.code === '0000') {
            const policyContent = result.data || '';
            document.getElementById('bucketPolicyContent').value = policyContent;

            // 根据策略内容自动判断类型并设置选中状态
            if (policyContent && policyContent.trim() !== '') {
                const policyType = determinePolicyType(policyContent);
                console.log('当前存储桶策略类型:', policyType);
                selectPolicyType(policyType);
            }
        } else {
            alert('查询失败: ' + (result.message || '未知错误'));
        }
    } catch (error) {
        alert('网络错误: ' + error.message);
    }
}

function selectPolicyType(type) {
    console.log('selectPolicyType 调用，type:', type);
    
    selectedPolicyType = type;
    
    // 移除所有选中状态
    document.querySelectorAll('#bucketPolicyModal .policy-type-option').forEach(opt => {
        opt.classList.remove('selected');
    });
    
    // 添加选中状态到目标按钮
    const targetOption = document.querySelector(`#bucketPolicyModal .policy-type-option[data-type="${type}"]`);
    console.log('找到的目标按钮:', targetOption);
    
    if (targetOption) {
        targetOption.classList.add('selected');
        console.log('已添加 selected 类');
    } else {
        console.error('未找到类型为', type, '的按钮');
        return;
    }

    const customGroup = document.getElementById('customTemplateGroup');
    console.log('customTemplateGroup:', customGroup);
    
    if (type === 'custom') {
        customGroup.style.display = 'block';
        console.log('显示自定义模板选择器');
    } else {
        customGroup.style.display = 'none';
        console.log('调用 applyPresetPolicy(', type, ')');
        applyPresetPolicy(type);
    }
}

function applyPresetPolicy(type) {
    let policy = '';
    
    console.log('applyPresetPolicy 调用，type:', type);
    
    switch(type) {
        case 'public':
            policy = `{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Principal": {"AWS": ["*"]},
      "Action": ["s3:GetBucketLocation", "s3:ListBucket", "s3:ListBucketMultipartUploads"],
      "Resource": ["arn:aws:s3:::${currentBucket}"]
    },
    {
      "Effect": "Allow",
      "Principal": {"AWS": ["*"]},
      "Action": ["s3:GetObject", "s3:ListMultipartUploadParts", "s3:PutObject", "s3:AbortMultipartUpload", "s3:DeleteObject"],
      "Resource": ["arn:aws:s3:::${currentBucket}/*"]
    }
  ]
}`;
            break;
        case 'readonly':
            policy = `{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Principal": {"AWS": ["*"]},
      "Action": ["s3:GetObject"],
      "Resource": ["arn:aws:s3:::${currentBucket}/*"]
    }
  ]
}`;
            break;
        case 'private':
            policy = `{
  "Version": "2012-10-17",
  "Statement": []
}`;
            break;
        default:
            console.warn('未知的策略类型:', type);
            return;
    }
    
    console.log('生成的策略:', policy);
    document.getElementById('bucketPolicyContent').value = policy;
}

function applyTemplateToPolicy() {
    const templateName = document.getElementById('customPolicyTemplate').value;
    const template = policyTemplates.find(t => t.templateName === templateName);
    if (template) {
        document.getElementById('bucketPolicyContent').value = template.policy.replace(/{bucket}/g, currentBucket);
    }
}

async function saveBucketPolicy() {
    const policy = document.getElementById('bucketPolicyContent').value;

    try {
        const response = await fetch('/oss/setBucketPolicy', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                bucketName: currentBucket,
                policy: policy
            })
        });
        const result = await response.json();
        if (result.code === '0000') {
            alert('✅ 策略保存成功');
            closeModal('bucketPolicyModal');
        } else {
            alert('❌ 保存失败: ' + (result.message || '未知错误'));
        }
    } catch (error) {
        alert('网络错误: ' + error.message);
    }
}

// ==================== 策略模板管理 ====================
async function refreshPolicyTemplates() {
    const btn = document.getElementById('refreshPoliciesBtn');
    if (btn) {
        btn.disabled = true;
        btn.innerHTML = '<span class="loading"></span> 刷新中...';
    }

    try {
        const response = await fetch('/oss/listPolicyTemplates');
        const result = await response.json();
        if (result.code === '0000' && result.data) {
            policyTemplates = result.data;
            displayPolicyTemplates(policyTemplates);
            updateBucketSelects(buckets);
        } else {
            alert('查询失败: ' + (result.message || '未知错误'));
        }
    } catch (error) {
        alert('网络错误: ' + error.message);
    } finally {
        if (btn) {
            btn.disabled = false;
            btn.innerHTML = '<span>🔄</span> 刷新';
        }
    }
}

function displayPolicyTemplates(templates) {
    const container = document.getElementById('policyTemplateList');
    if (templates.length === 0) {
        container.innerHTML = `
            <div class="empty-state">
                <div class="empty-state-icon">📋</div>
                暂无策略模板，点击"新建策略模板"创建
            </div>
        `;
        return;
    }

    container.innerHTML = templates.map(template => `
        <div class="policy-template-card">
            <div class="policy-template-name">${template.templateName}</div>
            <div class="policy-template-type ${template.policyType}">${template.description}</div>
            <div class="policy-template-actions">
                <button class="btn btn-primary btn-small" onclick="editPolicyTemplate('${template.templateName}')">✏️ 编辑</button>
                <button class="btn btn-danger btn-small" onclick="deletePolicyTemplate('${template.templateName}')">🗑️ 删除</button>
            </div>
        </div>
    `).join('');
}

function showCreatePolicyTemplateModal() {
    document.getElementById('createPolicyTemplateModal').classList.add('show');
    document.getElementById('policyTemplateName').value = '';
    document.getElementById('policyTemplateDesc').value = '';
    document.getElementById('templateCustomPolicy').value = '';
    selectedTemplatePolicyType = '';

    document.querySelectorAll('#createPolicyTemplateModal .policy-type-option').forEach(opt => {
        opt.classList.remove('selected');
    });
    document.getElementById('templateCustomPolicyGroup').style.display = 'none';
}

function selectTemplatePolicyType(type) {
    selectedTemplatePolicyType = type;
    document.querySelectorAll('#createPolicyTemplateModal .policy-type-option').forEach(opt => {
        opt.classList.remove('selected');
    });
    document.querySelector(`#createPolicyTemplateModal .policy-type-option[data-type="${type}"]`).classList.add('selected');

    const customGroup = document.getElementById('templateCustomPolicyGroup');
    if (type === 'custom') {
        customGroup.style.display = 'block';
    } else {
        customGroup.style.display = 'none';
    }
}

async function confirmCreatePolicyTemplate() {
    const templateName = document.getElementById('policyTemplateName').value.trim();
    const description = document.getElementById('policyTemplateDesc').value.trim();
    const policyType = selectedTemplatePolicyType;
    let policy = '';

    if (!templateName) {
        alert('请输入模板名称');
        return;
    }
    if (!description) {
        alert('请输入模板描述');
        return;
    }
    if (!policyType) {
        alert('请选择策略类型');
        return;
    }

    if (policyType === 'custom') {
        policy = document.getElementById('templateCustomPolicy').value.trim();
        if (!policy) {
            alert('请输入自定义策略内容');
            return;
        }
    }

    try {
        const response = await fetch('/oss/createPolicyTemplate', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                templateName,
                description,
                policyType,
                policy
            })
        });
        const result = await response.json();
        if (result.code === '0000') {
            alert('✅ 创建成功');
            closeModal('createPolicyTemplateModal');
            refreshPolicyTemplates();
        } else {
            alert('❌ 创建失败: ' + (result.message || '未知错误'));
        }
    } catch (error) {
        alert('网络错误: ' + error.message);
    }
}

async function editPolicyTemplate(templateName) {
    try {
        const response = await fetch(`/oss/getPolicyTemplate?templateName=${encodeURIComponent(templateName)}`);
        const result = await response.json();
        if (result.code === '0000' && result.data) {
            editingTemplate = result.data;
            document.getElementById('editPolicyTemplateModal').classList.add('show');
            document.getElementById('editPolicyTemplateName').value = result.data.templateName;
            document.getElementById('editPolicyTemplateDesc').value = result.data.description;
            document.getElementById('editPolicyTemplatePolicy').value = result.data.policy;
        } else {
            alert('查询失败: ' + (result.message || '未知错误'));
        }
    } catch (error) {
        alert('网络错误: ' + error.message);
    }
}

async function confirmEditPolicyTemplate() {
    if (!editingTemplate) return;

    const description = document.getElementById('editPolicyTemplateDesc').value.trim();
    const policy = document.getElementById('editPolicyTemplatePolicy').value.trim();

    if (!description) {
        alert('请输入模板描述');
        return;
    }
    if (!policy) {
        alert('请输入策略内容');
        return;
    }

    try {
        const response = await fetch('/oss/updatePolicyTemplate', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                templateName: editingTemplate.templateName,
                description,
                policy,
                policyType: editingTemplate.policyType
            })
        });
        const result = await response.json();
        if (result.code === '0000') {
            alert('✅ 更新成功');
            closeModal('editPolicyTemplateModal');
            refreshPolicyTemplates();
        } else {
            alert('❌ 更新失败: ' + (result.message || '未知错误'));
        }
    } catch (error) {
        alert('网络错误: ' + error.message);
    }
}

async function deletePolicyTemplate(templateName) {
    if (!confirm(`⚠️ 确定要删除策略模板 "${templateName}" 吗？`)) {
        return;
    }

    try {
        const response = await fetch(`/oss/deletePolicyTemplate/${encodeURIComponent(templateName)}`, { method: 'DELETE' });
        const result = await response.json();
        if (result.code === '0000') {
            alert('✅ 删除成功');
            refreshPolicyTemplates();
        } else {
            alert('❌ 删除失败: ' + (result.message || '未知错误'));
        }
    } catch (error) {
        alert('网络错误: ' + error.message);
    }
}

// ==================== 文件上传 ====================
function showUploadModal() {
    document.getElementById('uploadModal').classList.add('show');
    document.getElementById('uploadResult').classList.remove('show');
    setupDragAndDrop();
}

function setupDragAndDrop() {
    const uploadArea = document.getElementById('normalUploadArea');

    uploadArea.addEventListener('dragover', (e) => {
        e.preventDefault();
        uploadArea.classList.add('dragover');
    });

    uploadArea.addEventListener('dragleave', (e) => {
        e.preventDefault();
        uploadArea.classList.remove('dragover');
    });

    uploadArea.addEventListener('drop', (e) => {
        e.preventDefault();
        uploadArea.classList.remove('dragover');
        const files = e.dataTransfer.files;
        if (files.length > 0) {
            handleFileSelect({ target: { files: files } });
        }
    });
}

function switchUploadTab(type) {
    const tabs = document.querySelectorAll('.upload-modal .tabs-section button');
    tabs.forEach(t => t.classList.remove('active'));
    event.target.classList.add('active');

    document.getElementById('normalUpload').classList.toggle('active', type === 'normal');
    document.getElementById('chunkUpload').classList.toggle('active', type === 'chunk');

    // 普通上传和分片上传的底部按钮控制
    const uploadBtn = document.getElementById('uploadBtn');
    const modalActions = document.querySelector('#uploadModal .modal-actions');
    if (type === 'chunk') {
        uploadBtn.style.display = 'none';
        // 初始化分片上传器
        if (typeof reinitializeChunkUploader === 'function') {
            reinitializeChunkUploader();
        }
    } else {
        uploadBtn.style.display = 'inline-block';
    }
}

function handleFileSelect(event) {
    const file = event.target.files[0];
    if (file) {
        selectedFile = file;
        document.querySelector('.upload-area p').textContent = `✅ 已选择: ${file.name} (${formatFileSize(file.size)})`;
    }
}

async function uploadFile() {
    if (!selectedFile) {
        alert('请先选择文件');
        return;
    }

    const uploadPath = document.getElementById('uploadPath').value;
    const formData = new FormData();
    formData.append('file', selectedFile);
    formData.append('bucketName', currentBucket);

    try {
        const response = await fetch('/oss/uploadFile', {
            method: 'POST',
            body: formData
        });
        const result = await response.json();
        if (result.code === '0000' && result.data) {
            const resultBox = document.getElementById('uploadResult');
            const urlElement = document.getElementById('fileUrl');
            resultBox.classList.add('show');
            urlElement.href = result.data;
            urlElement.textContent = result.data;

            selectedFile = null;
            document.querySelector('.upload-area p').textContent = '点击或拖拽文件到此处上传';
            document.getElementById('fileInput').value = '';
            document.getElementById('uploadPath').value = '';

            refreshFiles();
        } else {
            alert('❌ 上传失败: ' + (result.message || '未知错误'));
        }
    } catch (error) {
        alert('网络错误: ' + error.message);
    }
}

function openChunkUpload() {
    // 获取分片上传标签按钮
    const tabs = document.querySelectorAll('.upload-modal .tabs-section button');
    const chunkTab = tabs[1]; // 第二个标签是分片上传

    if (chunkTab) {
        // 手动切换标签
        tabs.forEach(t => t.classList.remove('active'));
        chunkTab.classList.add('active');

        document.getElementById('normalUpload').classList.remove('active');
        document.getElementById('chunkUpload').classList.add('active');

        // 隐藏普通上传的"开始上传"按钮
        const uploadBtn = document.getElementById('uploadBtn');
        if (uploadBtn) {
            uploadBtn.style.display = 'none';
        }

        // 初始化分片上传器
        if (typeof reinitializeChunkUploader === 'function') {
            reinitializeChunkUploader();
        }

        // 预填存储桶名称
        const chunkBucketNameInput = document.getElementById('chunkBucketName');
        if (chunkBucketNameInput && currentBucket) {
            chunkBucketNameInput.value = currentBucket;
        }
    }
}

// ==================== 通用工具 ====================
function closeModal(modalId) {
    document.getElementById(modalId).classList.remove('show');
}

function formatFileSize(bytes) {
    if (bytes === 0) return '0 B';
    const k = 1024;
    const sizes = ['B', 'KB', 'MB', 'GB', 'TB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return Math.round(bytes / Math.pow(k, i) * 100) / 100 + ' ' + sizes[i];
}

// 点击模态框背景关闭
document.querySelectorAll('.modal').forEach(modal => {
    modal.addEventListener('click', (e) => {
        if (e.target === modal) {
            modal.classList.remove('show');
        }
    });
});

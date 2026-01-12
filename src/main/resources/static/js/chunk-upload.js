class ChunkUploader {
    constructor() {
        this.file = null;
        this.fileMd5 = '';
        this.uploadSessionId = ''; // 上传会话ID
        this.chunkSize = 5 * 1024 * 1024; // 默认5MB
        this.totalChunks = 0;
        this.uploadedChunks = new Set();
        this.isPaused = false;
        this.isCancelled = false;
        this.concurrency = 3;
        this.activeUploads = 0;
        this.uploadedBytes = 0;
        this.startTime = 0;
        this.lastUpdateBytes = 0;
        this.lastUpdateTime = 0;
        
        this.initElements();
        this.bindEvents();
    }
    
    initElements() {
        this.uploadArea = document.getElementById('chunkUploadArea');
        this.fileInput = document.getElementById('chunkFileInput');
        this.fileInfo = document.getElementById('chunkFileInfo');
        this.fileName = document.getElementById('chunkFileName');
        this.fileSize = document.getElementById('chunkFileSize');
        this.fileMd5 = document.getElementById('chunkFileMd5');
        this.chunkSizeInput = document.getElementById('chunkSize');
        this.bucketNameInput = document.getElementById('chunkBucketName');
        this.concurrencyInput = document.getElementById('chunkConcurrency');
        this.startBtn = document.getElementById('chunkStartBtn');
        this.pauseBtn = document.getElementById('chunkPauseBtn');
        this.cancelBtn = document.getElementById('chunkCancelBtn');
        this.retryBtn = document.getElementById('chunkRetryBtn');
        this.progressContainer = document.getElementById('chunkProgressContainer');
        this.progressText = document.getElementById('chunkProgressText');
        this.progressPercent = document.getElementById('chunkProgressPercent');
        this.progressFill = document.getElementById('chunkProgressFill');
        this.progressStatus = document.getElementById('chunkProgressStatus');
        this.uploadSpeed = document.getElementById('chunkUploadSpeed');
        this.uploadTime = document.getElementById('chunkUploadTime');
        this.chunkList = document.getElementById('chunkList');
        this.result = document.getElementById('chunkResult');
        this.fileUrl = document.getElementById('chunkFileUrl');
    }
    
    bindEvents() {
        this.uploadArea.addEventListener('click', () => this.fileInput.click());
        this.fileInput.addEventListener('change', (e) => this.handleFileSelect(e.target.files[0]));
        this.uploadArea.addEventListener('dragover', (e) => this.handleDragOver(e));
        this.uploadArea.addEventListener('dragleave', (e) => this.handleDragLeave(e));
        this.uploadArea.addEventListener('drop', (e) => this.handleDrop(e));
        this.startBtn.addEventListener('click', () => this.startUpload());
        this.pauseBtn.addEventListener('click', () => this.togglePause());
        this.cancelBtn.addEventListener('click', () => this.cancelUpload());
        this.retryBtn.addEventListener('click', () => this.retryFailed());

        // 分片大小输入框失焦校验
        this.chunkSizeInput.addEventListener('blur', () => this.validateChunkSize());
    }

    validateChunkSize() {
        const chunkSizeMB = parseInt(this.chunkSizeInput.value);
        const errorElement = document.getElementById('chunkSizeError');

        if (chunkSizeMB < 5) {
            errorElement.style.display = 'block';
            return false;
        } else {
            errorElement.style.display = 'none';
            return true;
        }
    }
    
    handleFileSelect(file) {
        if (!file) return;
        this.file = file;
        this.uploadSessionId = this.generateUUID();
        this.showFileInfo();
        this.calculateMd5();
    }
    
    generateUUID() {
        return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function(c) {
            const r = Math.random() * 16 | 0;
            const v = c === 'x' ? r : (r & 0x3 | 0x8);
            return v.toString(16);
        });
    }
    
    handleDragOver(e) {
        e.preventDefault();
        this.uploadArea.classList.add('dragover');
    }
    
    handleDragLeave(e) {
        e.preventDefault();
        this.uploadArea.classList.remove('dragover');
    }
    
    handleDrop(e) {
        e.preventDefault();
        this.uploadArea.classList.remove('dragover');
        const file = e.dataTransfer.files[0];
        if (file) {
            this.handleFileSelect(file);
        }
    }
    
    async calculateMd5() {
        if (this.fileMd5) {
            this.fileMd5.textContent = '计算中...';
        }
        try {
            const spark = new SparkMD5.ArrayBuffer();
            const chunkSize = 2 * 1024 * 1024;
            const chunks = Math.ceil(this.file.size / chunkSize);

            for (let i = 0; i < chunks; i++) {
                const start = i * chunkSize;
                const end = Math.min(start + chunkSize, this.file.size);
                const chunk = this.file.slice(start, end);
                const buffer = await chunk.arrayBuffer();
                spark.append(buffer);

                // 更新计算进度
                const percent = Math.round(((i + 1) / chunks) * 100);
                if (this.fileMd5) {
                    this.fileMd5.textContent = `计算中... ${percent}%`;
                }
            }

            this.fileMd5Value = spark.end();
            if (this.fileMd5) {
                this.fileMd5.textContent = this.fileMd5Value;
            }
            if (this.startBtn) {
                this.startBtn.disabled = false;
            }
        } catch (error) {
            console.error('计算MD5失败:', error);
            if (this.fileMd5) {
                this.fileMd5.textContent = '计算失败';
            }
        }
    }
    
    showFileInfo() {
        if (this.fileInfo) {
            this.fileInfo.classList.add('show');
            this.fileName.textContent = this.file.name;
            this.fileSize.textContent = this.formatFileSize(this.file.size);
        }
        if (this.result) {
            this.result.classList.remove('show');
        }
        if (this.progressContainer) {
            this.progressContainer.classList.remove('show');
        }
    }
    
    formatFileSize(bytes) {
        if (bytes === 0) return '0 B';
        const k = 1024;
        const sizes = ['B', 'KB', 'MB', 'GB', 'TB'];
        const i = Math.floor(Math.log(bytes) / Math.log(k));
        return Math.round(bytes / Math.pow(k, i) * 100) / 100 + ' ' + sizes[i];
    }
    
    async startUpload() {
        if (!this.file || !this.fileMd5Value) {
            alert('请先选择文件并等待MD5计算完成');
            return;
        }

        // 校验分片大小不能低于 5MB
        if (!this.validateChunkSize()) {
            return;
        }

        const chunkSizeMB = parseInt(this.chunkSizeInput.value);
        this.chunkSize = chunkSizeMB * 1024 * 1024;
        this.totalChunks = Math.ceil(this.file.size / this.chunkSize);
        this.concurrency = parseInt(this.concurrencyInput.value);

        this.isPaused = false;
        this.isCancelled = false;
        this.uploadedChunks.clear();
        this.uploadedBytes = 0;
        this.startTime = Date.now();
        this.lastUpdateBytes = 0;
        this.lastUpdateTime = this.startTime;

        if (this.startBtn) {
            this.startBtn.disabled = true;
        }
        if (this.pauseBtn) {
            this.pauseBtn.disabled = false;
        }
        if (this.cancelBtn) {
            this.cancelBtn.disabled = false;
        }
        if (this.retryBtn) {
            this.retryBtn.disabled = true;
        }

        if (this.progressContainer) {
            this.progressContainer.classList.add('show');
        }
        this.initChunkList();

        try {
            await this.initUpload();
            await this.uploadChunks();
            await this.completeUpload();
        } catch (error) {
            console.error('上传失败:', error);
            alert('上传失败: ' + error.message);
        } finally {
            if (this.startBtn) {
                this.startBtn.disabled = false;
            }
            if (this.pauseBtn) {
                this.pauseBtn.disabled = true;
            }
            if (this.cancelBtn) {
                this.cancelBtn.disabled = true;
            }
            if (this.pauseBtn) {
                this.pauseBtn.textContent = '暂停上传';
            }
        }
    }
    
    async initUpload() {
        const response = await fetch('/oss/initChunkUpload', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                fileName: this.file.name,
                fileMd5: this.fileMd5Value,
                totalSize: this.file.size,
                chunkSize: this.chunkSize,
                uploadSessionId: this.uploadSessionId
            })
        });
        
        const result = await response.json();
        if (result.code === "0000" && result.data) {
            result.data.uploadedChunks.forEach(chunkIndex => {
                this.uploadedChunks.add(chunkIndex);
                this.updateChunkStatus(chunkIndex, 'completed');
            });
            this.updateProgress();
        }
    }
    
    async uploadChunks() {
        const chunksToUpload = [];
        for (let i = 0; i < this.totalChunks; i++) {
            if (!this.uploadedChunks.has(i)) {
                chunksToUpload.push(i);
            }
        }
        
        if (chunksToUpload.length === 0) {
            return;
        }
        
        const uploadQueue = [...chunksToUpload];
        
        const uploadNext = async () => {
            if (this.isCancelled || this.isPaused) {
                return;
            }
            
            if (uploadQueue.length === 0) {
                return;
            }
            
            if (this.activeUploads >= this.concurrency) {
                return;
            }
            
            const chunkIndex = uploadQueue.shift();
            this.activeUploads++;
            
            this.updateChunkStatus(chunkIndex, 'uploading');
            
            try {
                await this.uploadChunk(chunkIndex);
                this.uploadedChunks.add(chunkIndex);
                this.updateChunkStatus(chunkIndex, 'completed');
                this.updateProgress();
            } catch (error) {
                console.error(`分片 ${chunkIndex} 上传失败:`, error);
                this.updateChunkStatus(chunkIndex, 'failed');
            } finally {
                this.activeUploads--;
                if (!this.isPaused && !this.isCancelled && uploadQueue.length > 0) {
                    uploadNext();
                }
            }
        };
        
        const workers = Array(Math.min(this.concurrency, uploadQueue.length)).fill(null);
        await Promise.all(workers.map(() => uploadNext()));
        
        while (uploadQueue.length > 0 && !this.isPaused && !this.isCancelled) {
            await new Promise(resolve => setTimeout(resolve, 100));
            await uploadNext();
        }
        
        if (!this.isPaused && !this.isCancelled) {
            await new Promise(resolve => {
                const checkDone = setInterval(() => {
                    if (this.activeUploads === 0) {
                        clearInterval(checkDone);
                        resolve();
                    }
                }, 100);
            });
        }
    }
    
    uploadChunk(chunkIndex) {
        return new Promise((resolve, reject) => {
            const start = chunkIndex * this.chunkSize;
            const end = Math.min(start + this.chunkSize, this.file.size);
            const chunk = this.file.slice(start, end);
            
            const formData = new FormData();
            formData.append('fileName', this.file.name);
            formData.append('fileMd5', this.fileMd5Value);
            formData.append('uploadSessionId', this.uploadSessionId);
            formData.append('chunkNumber', chunkIndex);
            formData.append('totalChunks', this.totalChunks);
            formData.append('chunkSize', this.chunkSize);
            formData.append('totalSize', this.file.size);
            formData.append('file', chunk);
            
            const xhr = new XMLHttpRequest();
            xhr.open('POST', '/oss/uploadChunk', true);
            
            xhr.upload.onprogress = (e) => {
                if (e.lengthComputable) {
                    const percent = Math.round((e.loaded / e.total) * 100);
                    // 更新分片的上传进度
                    const chunkItem = document.getElementById(`chunk-${chunkIndex}`);
                    if (chunkItem) {
                        const statusDiv = chunkItem.querySelector('.chunk-status');
                        if (statusDiv && statusDiv.textContent === '上传中') {
                            statusDiv.textContent = `上传中 ${percent}%`;
                        }
                    }
                }
            };
            
            xhr.onload = () => {
                if (xhr.status === 200) {
                    try {
                        const result = JSON.parse(xhr.responseText);
                        if (result.code === "0000") {
                            this.uploadedBytes += chunk.size;
                            this.updateSpeed();
                            resolve();
                        } else {
                            reject(new Error(result.message || '上传失败'));
                        }
                    } catch (e) {
                        reject(new Error('解析响应失败'));
                    }
                } else {
                    reject(new Error(`HTTP ${xhr.status}`));
                }
            };
            
            xhr.onerror = () => reject(new Error('网络错误'));
            xhr.ontimeout = () => reject(new Error('请求超时'));
            xhr.timeout = 60000; // 60秒超时
            
            xhr.send(formData);
        });
    }
    
    async completeUpload() {
        const bucketName = this.bucketNameInput.value || 'public';

        const response = await fetch('/oss/completeChunkUpload', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                fileName: this.file.name,
                fileMd5: this.fileMd5Value,
                totalChunks: this.totalChunks,
                bucketName: bucketName,
                uploadSessionId: this.uploadSessionId
            })
        });

        if (!response.ok) {
            const errorText = await response.text();
            console.error('合并请求失败，状态码:', response.status, '响应内容:', errorText);
            throw new Error(`合并失败 (HTTP ${response.status}): ${errorText}`);
        }

        const result = await response.json();
        console.log('合并响应:', result);

        if (result.code === "0000") {
            this.showResult(result.data);
        } else {
            console.error('合并失败，错误码:', result.code, '错误信息:', result.message);
            throw new Error(result.message || '合并失败');
        }
    }
    
    updateProgress() {
        const uploaded = this.uploadedChunks.size;
        const percent = Math.round((uploaded / this.totalChunks) * 100);

        if (this.progressText) {
            this.progressText.textContent = `${uploaded} / ${this.totalChunks} 分片`;
        }
        if (this.progressPercent) {
            this.progressPercent.textContent = percent + '%';
        }
        if (this.progressFill) {
            this.progressFill.style.width = percent + '%';
        }
        if (this.progressStatus) {
            this.progressStatus.textContent = percent + '%';
        }
    }

    updateSpeed() {
        const now = Date.now();
        const elapsed = (now - this.lastUpdateTime) / 1000;

        if (elapsed >= 0.5) {
            const bytesPerSecond = Math.round((this.uploadedBytes - this.lastUpdateBytes) / elapsed);
            if (this.uploadSpeed) {
                this.uploadSpeed.textContent = `速度: ${this.formatFileSize(bytesPerSecond)}/s`;
            }
            this.lastUpdateBytes = this.uploadedBytes;
            this.lastUpdateTime = now;
        }

        const totalElapsed = Math.floor((now - this.startTime) / 1000);
        const minutes = Math.floor(totalElapsed / 60).toString().padStart(2, '0');
        const seconds = (totalElapsed % 60).toString().padStart(2, '0');
        if (this.uploadTime) {
            this.uploadTime.textContent = `用时: ${minutes}:${seconds}`;
        }
    }
    
    initChunkList() {
        if (this.chunkList) {
            this.chunkList.innerHTML = '';
            for (let i = 0; i < this.totalChunks; i++) {
                const chunkItem = document.createElement('div');
                chunkItem.className = 'chunk-item';
                chunkItem.id = `chunk-${i}`;
                chunkItem.innerHTML = `
                    <div class="chunk-number">分片 ${i + 1}</div>
                    <div class="chunk-status pending">等待中</div>
                    <div class="chunk-icon">⏳</div>
                `;
                this.chunkList.appendChild(chunkItem);
            }
        }
    }
    
    updateChunkStatus(chunkIndex, status) {
        const chunkItem = document.getElementById(`chunk-${chunkIndex}`);
        if (chunkItem) {
            const statusDiv = chunkItem.querySelector('.chunk-status');
            const iconDiv = chunkItem.querySelector('.chunk-icon');
            
            statusDiv.className = `chunk-status ${status}`;
            iconDiv.textContent = status === 'completed' ? '✅' : 
                                   status === 'uploading' ? '📤' : 
                                   status === 'failed' ? '❌' : '⏳';
            statusDiv.textContent = status === 'completed' ? '已完成' : 
                                    status === 'uploading' ? '上传中' : 
                                    status === 'failed' ? '失败' : '等待中';
        }
    }
    
    togglePause() {
        this.isPaused = !this.isPaused;
        if (this.pauseBtn) {
            this.pauseBtn.textContent = this.isPaused ? '继续上传' : '暂停上传';
        }

        if (!this.isPaused) {
            this.uploadChunks();
        }
    }
    
    async cancelUpload() {
        if (confirm('确定要取消上传吗？已上传的分片将被删除。')) {
            this.isCancelled = true;
            this.reset();
            
            try {
                await fetch(`/oss/cancelChunkUpload?fileMd5=${this.fileMd5Value}&uploadSessionId=${this.uploadSessionId}`, {
                    method: 'DELETE'
                });
            } catch (error) {
                console.error('取消上传失败:', error);
            }
        }
    }
    
    async retryFailed() {
        const failedChunks = [];
        for (let i = 0; i < this.totalChunks; i++) {
            if (!this.uploadedChunks.has(i)) {
                failedChunks.push(i);
            }
        }

        if (failedChunks.length === 0) {
            alert('没有失败的分片需要重试');
            return;
        }

        this.isPaused = false;
        if (this.pauseBtn) {
            this.pauseBtn.textContent = '暂停上传';
        }
        this.isCancelled = false;

        if (this.retryBtn) {
            this.retryBtn.disabled = true;
        }
        if (this.startBtn) {
            this.startBtn.disabled = true;
        }

        await this.uploadChunks();

        if (this.uploadedChunks.size === this.totalChunks) {
            await this.completeUpload();
        } else {
            if (this.retryBtn) {
                this.retryBtn.disabled = false;
            }
        }
    }
    
    showResult(url) {
        if (this.result) {
            this.result.classList.add('show');
            this.fileUrl.href = url;
            this.fileUrl.textContent = url;
        }

        if (this.startBtn) {
            this.startBtn.disabled = false;
        }
        if (this.pauseBtn) {
            this.pauseBtn.disabled = true;
        }
        if (this.cancelBtn) {
            this.cancelBtn.disabled = true;
        }
        if (this.pauseBtn) {
            this.pauseBtn.textContent = '暂停上传';
        }
    }
    
    reset() {
        this.file = null;
        this.fileMd5Value = '';
        this.uploadedChunks.clear();
        this.isPaused = false;
        this.isCancelled = false;

        if (this.fileInfo) {
            this.fileInfo.classList.remove('show');
        }
        if (this.progressContainer) {
            this.progressContainer.classList.remove('show');
        }
        if (this.result) {
            this.result.classList.remove('show');
        }

        if (this.startBtn) {
            this.startBtn.disabled = true;
        }
        if (this.pauseBtn) {
            this.pauseBtn.disabled = true;
        }
        if (this.cancelBtn) {
            this.cancelBtn.disabled = true;
        }
        if (this.retryBtn) {
            this.retryBtn.disabled = true;
        }
        if (this.pauseBtn) {
            this.pauseBtn.textContent = '暂停上传';
        }

        if (this.fileInput) {
            this.fileInput.value = '';
        }
    }
}

// 全局变量，用于存储 ChunkUploader 实例
let chunkUploaderInstance = null;

document.addEventListener('DOMContentLoaded', () => {
    // 延迟初始化，确保DOM完全加载
    setTimeout(() => {
        // 检查分片上传元素是否存在
        const chunkUploadArea = document.getElementById('chunkUploadArea');
        if (chunkUploadArea) {
            chunkUploaderInstance = new ChunkUploader();
        }
    }, 100);
});

// 重新初始化分片上传器
function reinitializeChunkUploader() {
    if (chunkUploaderInstance) {
        // 如果已经存在实例，不重复创建
        return;
    }

    const chunkUploadArea = document.getElementById('chunkUploadArea');
    if (chunkUploadArea) {
        chunkUploaderInstance = new ChunkUploader();
    }
}

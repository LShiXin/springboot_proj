Vue.createApp({
    data() {
        return {
            tasks: [], // 任务列表
            isLoading: false,
            addingTask: false,
            newTask: {
                name: '',
                keywords: '',
                startTime: '',
                endTime: '',
                intervalMinutes: '',
                enabled: false,
                links: []
            }
        };
    },
    mounted() {
        this.loadUserTasks();
    },
    methods: {
        handleAddTask() {
            this.addingTask = true;
            this.newTask = { name: '', keyword: '', startTime: '', endTime: '', interval: '', active: true, links: [] };
        },
         async loadUserTasks() {
            var thit = this;
            // 判断 tasks 是否已存在有效任务（如有 id 字段的对象）
            if (this.tasks.length > 0 && this.tasks.some(t => t && t.id)) {
                return; // 如果已有任务，则不重新加载
            }
            var token = localStorage.getItem('token');
            if (!token) {
                alert('登录状态失效，请先登录');
                return;
            }
            try {
                const res = await fetch('http://localhost:8080/monitottask/getbyuser', {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json',
                        'Authorization': 'Bearer ' + token
                    }
                });
                if (!res.ok) throw new Error('获取用户任务失败');
                const result = await res.json();
                thit.tasks = result.data; // 直接覆盖任务列表
            } catch (err) {
                alert('获取用户任务失败：' + err.message);
            }
        },
        async confirmAddTask() {
            if (!this.newTask.name) {
                alert('请填写任务名称');
                return;
            }
            if (!this.newTask.keyword) {
                alert('请填写关键字');
                return;
            }
            if (!this.newTask.startTime) {
                alert('请选择开始时间');
                return;
            }
            if (!this.newTask.endTime) {
                alert('请选择结束时间');
                return;
            }
            if (!this.newTask.interval || this.newTask.interval < 1) {
                alert('请填写执行间隔（分钟，正整数）');
                return;
            }
            await this.confirmAddTaskApi(this.newTask);
        },
       
        async confirmAddTaskApi(taskData) {
            // 构造后端需要的任务对象
            var thit = this
            var token = localStorage.getItem('token');
            if (!token) {
                alert('登录状态失效，请先登录');
                return;
            }
            const payload = {
                name: taskData.name,
                keywords: taskData.keywords,
                enabled: taskData.enabled,
                // 定时任务相关字段
                scheduleConfig: {
                    startTime: taskData.startTime,
                    endTime: taskData.endTime,
                    intervalMillis: taskData.intervalMinutes * 60 * 1000 // 转为毫秒
                }
            };
            try {
                const res = await fetch('http://localhost:8080/monitottask/add', {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json',
                        'Authorization': 'Bearer ' + token
                    },
                    body: JSON.stringify(payload)
                });
               
                if (!res.ok) throw new Error('添加任务失败');
                const result = await res.json();    
                // 添加到本地列表
                console.log('添加任务响应:', result);
                var new_task = {
                    id: result.data.id,
                    name: result.data.name,
                    keyword: result.data.keywords,
                    startTime: result.data.start_time,
                    endTime: result.data.end_time,
                    interval: result.data.intervalMillis,
                    active: result.data.enabled,
                }
                console.log('当前任务列表:', thit.tasks);
                thit.tasks.unshift(new_task);
                thit.addingTask = false;
                alert('任务添加成功');
            } catch (err) {
                alert('任务添加失败：' + err.message);
            }
        },
        cancelAddTask() {
            this.addingTask = false;
        },
        handleToggleLinks(taskId) {
            this.$set(this.showLinks, taskId, !this.showLinks[taskId]);
        },
        handleActivate(task) {
            task.active = !task.active;
        },
        handleEdit(task) {
            alert('编辑任务功能开发中');
        },
        async handleDelete(taskId) {
            console.log('准备删除任务，ID:', taskId);
            var thit = this;
            if (!confirm('确定要删除该任务吗？')) {
                return;
            }
            
            var token = localStorage.getItem('token');
            console.log('删除任务，使用 token:', token);
            if (!token) {
                alert('登录状态失效，请先登录');
                return;
            }
            
            try {
                const res = await fetch('http://localhost:8080/monitottask/handleDelete', {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json',
                        'Authorization': 'Bearer ' + token
                    },
                    body: JSON.stringify(taskId)
                });
                
                if (!res.ok) {
                    const errorData = await res.json();
                    throw new Error(errorData.message || '删除任务失败');
                }
                
                const result = await res.json();
                if (result.code === 200 && result.data === true) {
                    // 从前端列表中移除已删除的任务
                    thit.tasks = thit.tasks.filter(t => t.id !== taskId);
                    alert('任务删除成功');
                } else {
                    alert('删除失败：' + (result.message || '未知错误'));
                }
            } catch (err) {
                alert('删除任务失败：' + err.message);
            }
        },
        handleAddLink(task) {
            alert('添加扫描链接功能开发中——aaaaa');
        },
        handleEditLink(task, linkIdx) {
            alert('编辑扫描链接功能开发中');
        },
        handleDeleteLink(task, linkIdx) {
            if (confirm('确定要删除该扫描链接吗？')) {
                task.links.splice(linkIdx, 1);
            }
        }
    }
}).mount('#monitorManagerApp');

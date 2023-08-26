package com.github.novicezk.midjourney.service.store;

import cn.hutool.cache.CacheUtil;
import cn.hutool.cache.impl.TimedCache;
import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.stream.StreamUtil;
import com.github.novicezk.midjourney.service.TaskStoreService;
import com.github.novicezk.midjourney.support.Task;
import com.github.novicezk.midjourney.support.TaskCondition;
import com.github.novicezk.midjourney.util.AliyunOssUtil;

import java.time.Duration;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;


public class InMemoryTaskStoreServiceImpl implements TaskStoreService {
	private final TimedCache<String, Task> taskMap;
	
	@Autowired
	private AliyunOssUtil aliyunOssUtil;

	public InMemoryTaskStoreServiceImpl(Duration timeout) {
		this.taskMap = CacheUtil.newTimedCache(timeout.toMillis());
	}

	@Override
	public void save(Task task) {
		if(task != null && task.getImageUrl() != null && !"".equals(task.getImageUrl())) {
			aliyunOssUtil.uploadImg(task.getImageUrl());
		}
		this.taskMap.put(task.getId(), task);
	}

	@Override
	public void delete(String key) {
		this.taskMap.remove(key);
	}

	@Override
	public Task get(String key) {
		Task task = this.taskMap.get(key);
		if(task != null && task.getImageUrl() != null && !"".equals(task.getImageUrl())) {
			aliyunOssUtil.uploadImg(task.getImageUrl());
		}
		return task;
	}

	@Override
	public List<Task> list() {
		return ListUtil.toList(this.taskMap.iterator());
	}

	@Override
	public List<Task> list(TaskCondition condition) {
		return StreamUtil.of(this.taskMap.iterator()).filter(condition).toList();
	}

	@Override
	public Task findOne(TaskCondition condition) {
		return StreamUtil.of(this.taskMap.iterator()).filter(condition).findFirst().orElse(null);
	}

}

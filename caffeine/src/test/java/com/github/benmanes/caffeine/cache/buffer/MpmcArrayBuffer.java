/*
 * Copyright 2015 Ben Manes. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.benmanes.caffeine.cache.buffer;

import java.util.concurrent.locks.Lock;

import org.jctools.queues.MpmcArrayQueue;

import com.github.benmanes.caffeine.locks.NonReentrantLock;

/**
 * @author ben.manes@gmail.com (Ben Manes)
 */
final class MpmcArrayBuffer implements ReadBuffer {
  final MpmcArrayQueue<Boolean> queue;
  final Lock evictionLock;
  long drained;

  MpmcArrayBuffer() {
    evictionLock = new NonReentrantLock();
    queue = new MpmcArrayQueue<>(MAX_SIZE);
  }

  @Override
  public boolean record() {
    return queue.offer(Boolean.TRUE);
  }

  @Override
  public void drain() {
    if (evictionLock.tryLock()) {
      while (queue.poll() != null) {
        drained++;
      }
      evictionLock.unlock();
    }
  }

  @Override
  public long recorded() {
    return drained() + queue.size();
  }

  @Override
  public long drained() {
    evictionLock.lock();
    try {
      return drained;
    } finally {
      evictionLock.unlock();
    }
  }
}

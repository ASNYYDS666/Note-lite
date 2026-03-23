This file is a merged representation of the entire codebase, combined into a single document by Repomix.

# File Summary

## Purpose
This file contains a packed representation of the entire repository's contents.
It is designed to be easily consumable by AI systems for analysis, code review,
or other automated processes.

## File Format
The content is organized as follows:
1. This summary section
2. Repository information
3. Directory structure
4. Repository files (if enabled)
5. Multiple file entries, each consisting of:
  a. A header with the file path (## File: path/to/file)
  b. The full contents of the file in a code block

## Usage Guidelines
- This file should be treated as read-only. Any changes should be made to the
  original repository files, not this packed version.
- When processing this file, use the file path to distinguish
  between different files in the repository.
- Be aware that this file may contain sensitive information. Handle it with
  the same level of security as you would the original repository.

## Notes
- Some files may have been excluded based on .gitignore rules and Repomix's configuration
- Binary files are not included in this packed representation. Please refer to the Repository Structure section for a complete list of file paths, including binary files
- Files matching patterns in .gitignore are excluded
- Files matching default ignore patterns are excluded
- Files are sorted by Git change count (files with more changes are at the bottom)

# Directory Structure
```
note-service/.gitattributes
note-service/.gitignore
note-service/.mvn/wrapper/maven-wrapper.properties
note-service/mvnw
note-service/mvnw.cmd
note-service/pom.xml
note-service/src/main/java/com/note/service/common/config/CorsConfig.java
note-service/src/main/java/com/note/service/common/config/JwtAuthenticationFilter.java
note-service/src/main/java/com/note/service/common/config/MybatisPlusConfig.java
note-service/src/main/java/com/note/service/common/config/RedisConfig.java
note-service/src/main/java/com/note/service/common/config/SecurityConfig.java
note-service/src/main/java/com/note/service/common/exception/BusinessException.java
note-service/src/main/java/com/note/service/common/exception/GlobalExceptionHandler.java
note-service/src/main/java/com/note/service/common/util/JwtUtils.java
note-service/src/main/java/com/note/service/common/vo/Result.java
note-service/src/main/java/com/note/service/config/SwaggerConfig.java
note-service/src/main/java/com/note/service/controller/NoteController.java
note-service/src/main/java/com/note/service/controller/UserController.java
note-service/src/main/java/com/note/service/dto/LoginDTO.java
note-service/src/main/java/com/note/service/dto/NoteDTO.java
note-service/src/main/java/com/note/service/dto/NoteQueryDTO.java
note-service/src/main/java/com/note/service/dto/UserRegisterDTO.java
note-service/src/main/java/com/note/service/entity/NoteEntity.java
note-service/src/main/java/com/note/service/entity/NoteTagEntity.java
note-service/src/main/java/com/note/service/entity/UserEntity.java
note-service/src/main/java/com/note/service/mapper/NoteMapper.java
note-service/src/main/java/com/note/service/mapper/NoteTagMapper.java
note-service/src/main/java/com/note/service/mapper/UserMapper.java
note-service/src/main/java/com/note/service/NoteServiceApplication.java
note-service/src/main/java/com/note/service/service/NoteService.java
note-service/src/main/java/com/note/service/service/UserService.java
note-service/src/main/resources/application.yml
note-service/src/main/resources/db/migration/V2__create_note_and_tag.sql
note-service/src/test/java/com/note/service/NoteServiceApplicationTests.java
note-ui/.gitignore
note-ui/index.html
note-ui/package.json
note-ui/public/vite.svg
note-ui/src/App.vue
note-ui/src/components/MarkdownEditor.vue
note-ui/src/components/TagFilter.vue
note-ui/src/counter.ts
note-ui/src/main.js
note-ui/src/router/index.js
note-ui/src/store/user.js
note-ui/src/style.css
note-ui/src/typescript.svg
note-ui/src/utils/request.js
note-ui/src/views/Layout.vue
note-ui/src/views/Login.vue
note-ui/src/views/NoteEdit.vue
note-ui/src/views/NoteList.vue
note-ui/src/views/RecycleBin.vue
note-ui/src/views/Register.vue
note-ui/tsconfig.json
note-ui/vite.config.js
package.json
README.md
```

# Files

## File: note-service/src/main/java/com/note/service/common/config/CorsConfig.java
````java
package com.note.service.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

/**
 * 跨域配置
 * 允许前端 localhost:5173 / localhost:3000 访问后端 API
 *
 * 为什么需要这个配置？
 * 1. 浏览器同源策略禁止不同端口的请求
 * 2. 前后端分离项目必然跨域（前端5173，后端8080）
 * 3. 必须正确处理 OPTIONS 预检请求
 */
@Configuration
public class CorsConfig {

    @Bean
    public CorsFilter corsFilter() {
        // 1. 创建 CORS 配置对象
        CorsConfiguration config = new CorsConfiguration();

        // ========== 允许的源 ==========
        // 允许前端开发服务器地址（Vite 默认端口 5173）
        config.addAllowedOrigin("http://localhost:5173");
        // 允许 React 或其他前端常用端口（可选，不影响功能）
        config.addAllowedOrigin("http://localhost:3000");
        // 如果需要部署到线上，可以添加域名
        // config.addAllowedOrigin("https://note.yourdomain.com");

        // ========== 允许的请求头 ==========
        // 允许所有请求头（包括 Authorization、Content-Type 等）
        config.addAllowedHeader("*");

        // ========== 允许的HTTP方法 ==========
        // 允许所有方法（GET, POST, PUT, DELETE, OPTIONS 等）
        config.addAllowedMethod("*");

        // ========== 允许携带凭证 ==========
        // 允许携带 Cookie 和 Authorization 头
        config.setAllowCredentials(true);

        // ========== 暴露响应头 ==========
        // 让前端能读取 Authorization 头（如果 JWT Token 放在 header 里）
        config.addExposedHeader("Authorization");

        // ========== 预检请求缓存 ==========
        // 预检请求结果缓存 1 小时（3600秒），减少 OPTIONS 请求次数
        config.setMaxAge(3600L);

        // 2. 为所有接口注册 CORS 配置
        // 使用 "/**" 匹配所有路径，包括 Swagger 文档等非 /api 路径
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        // 3. 返回 CORS 过滤器
        // 这个过滤器会自动注册到 Spring 过滤器链的最前面
        // 确保 OPTIONS 预检请求在到达 Security 之前就被正确处理
        return new CorsFilter(source);
    }
}
````

## File: note-ui/src/components/TagFilter.vue
````vue
//day05新建标签筛选组件
<template>
  <div class="tag-filter">
    <el-select
        v-model="selectedTags"
        multiple
        collapse-tags
        collapse-tags-tooltip
        placeholder="按标签筛选"
        clearable
        @change="handleChange"
    >
      <el-option
          v-for="tag in allTags"
          :key="tag"
          :label="tag"
          :value="tag"
      />
    </el-select>

    <el-radio-group v-model="matchMode" size="small" @change="handleChange">
      <el-radio-button label="ANY">任意标签</el-radio-button>
      <el-radio-button label="ALL">全部标签</el-radio-button>
    </el-radio-group>
  </div>
</template>

<script setup>
import { ref, watch } from 'vue'

const props = defineProps({
  modelValue: {
    type: Array,
    default: () => []
  },
  matchMode: {
    type: String,
    default: 'ANY'
  },
  allTags: {
    type: Array,
    default: () => []
  }
})

const emit = defineEmits(['update:modelValue', 'update:matchMode', 'change'])

const selectedTags = ref(props.modelValue)
const matchMode = ref(props.matchMode)

watch(() => props.modelValue, (val) => {
  selectedTags.value = val
})

watch(() => props.matchMode, (val) => {
  matchMode.value = val
})

const handleChange = () => {
  emit('update:modelValue', selectedTags.value)
  emit('update:matchMode', matchMode.value)
  emit('change', {
    tags: selectedTags.value,
    matchMode: matchMode.value
  })
}
</script>

<style scoped>
.tag-filter {
  display: flex;
  gap: 10px;
  align-items: center;
}

.tag-filter .el-select {
  width: 200px;
}
</style>
````

## File: note-ui/src/views/RecycleBin.vue
````vue
<template>
  <div class="recycle-bin">
    <div class="header">
      <h3>回收站</h3>
      <el-button type="danger" :loading="clearing" @click="handleClearAll">
        清空回收站
      </el-button>
    </div>

    <el-empty v-if="!loading && notes.length === 0" description="回收站是空的" />

    <div v-else class="list">
      <el-card v-for="note in notes" :key="note.id" class="note-card">
        <h4 class="title">{{ note.title }}</h4>
        <p class="summary">{{ note.summary || '暂无摘要' }}</p>
        <div class="meta">
          <el-tag v-for="tag in note.tags" :key="tag" size="small" class="tag">
            {{ tag }}
          </el-tag>
          <span class="time">删除于 {{ formatTime(note.deletedAt) }}</span>
        </div>
        <div class="actions">
          <el-button size="small" type="primary" @click="restoreNote(note.id)">
            恢复
          </el-button>
          <el-button size="small" type="danger" @click="permanentDelete(note.id)">
            永久删除
          </el-button>
        </div>
      </el-card>
    </div>

    <el-pagination
        v-if="total > 0"
        v-model:current-page="pageNum"
        :page-size="pageSize"
        :total="total"
        layout="prev, pager, next"
        @current-change="loadRecycle"
    />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '@/utils/request'

const notes = ref([])
const loading = ref(false)
const clearing = ref(false)
const pageNum = ref(1)
const pageSize = ref(20)
const total = ref(0)

const loadRecycle = async () => {
  loading.value = true
  try {
    const res = await request.get('/note/recycle/page', {
      params: {
        pageNum: pageNum.value,
        pageSize: pageSize.value
      }
    })
    notes.value = res.data.records
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}

const restoreNote = async (id) => {
  try {
    await request.put(`/note/${id}/restore`)
    ElMessage.success('已恢复')
    loadRecycle()
  } catch (error) {
    console.error('恢复失败:', error)
  }
}

const permanentDelete = async (id) => {
  try {
    await ElMessageBox.confirm('确定永久删除该笔记吗？此操作不可恢复', '警告', {
      type: 'warning'
    })
    await request.delete(`/note/${id}?permanent=true`)
    ElMessage.success('已永久删除')
    loadRecycle()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除失败:', error)
    }
  }
}

const handleClearAll = async () => {
  try {
    await ElMessageBox.confirm('确定清空回收站吗？所有笔记将永久删除', '警告', {
      type: 'warning'
    })
    clearing.value = true
    await request.delete('/note/recycle/clear')
    ElMessage.success('回收站已清空')
    loadRecycle()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('清空失败:', error)
    }
  } finally {
    clearing.value = false
  }
}

const formatTime = (time) => {
  if (!time) return ''
  return new Date(time).toLocaleString('zh-CN')
}

onMounted(loadRecycle)
</script>

<style scoped>
.recycle-bin {
  max-width: 1200px;
  margin: 0 auto;
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.note-card {
  margin-bottom: 15px;
  position: relative;
}

.title {
  margin: 0 0 10px 0;
  color: #303133;
}

.summary {
  color: #606266;
  font-size: 14px;
  margin-bottom: 10px;
  line-height: 1.5;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.meta {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
  margin-bottom: 15px;
}

.tag {
  margin-right: 5px;
}

.time {
  color: #909399;
  font-size: 12px;
  margin-left: auto;
}

.actions {
  display: flex;
  gap: 10px;
  justify-content: flex-end;
  border-top: 1px solid #ebeef5;
  padding-top: 15px;
}
</style>
````

## File: package.json
````json
{
  "dependencies": {
    "lodash-es": "^4.17.23"
  }
}
````

## File: note-service/.gitattributes
````
/mvnw text eol=lf
*.cmd text eol=crlf
````

## File: note-service/.gitignore
````
HELP.md
target/
.mvn/wrapper/maven-wrapper.jar
!**/src/main/**/target/
!**/src/test/**/target/

### STS ###
.apt_generated
.classpath
.factorypath
.project
.settings
.springBeans
.sts4-cache

### IntelliJ IDEA ###
.idea
*.iws
*.iml
*.ipr

### NetBeans ###
/nbproject/private/
/nbbuild/
/dist/
/nbdist/
/.nb-gradle/
build/
!**/src/main/**/build/
!**/src/test/**/build/

### VS Code ###
.vscode/
````

## File: note-service/.mvn/wrapper/maven-wrapper.properties
````
wrapperVersion=3.3.4
distributionType=only-script
distributionUrl=https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/3.9.12/apache-maven-3.9.12-bin.zip
````

## File: note-service/mvnw
````
#!/bin/sh
# ----------------------------------------------------------------------------
# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#    http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.
# ----------------------------------------------------------------------------

# ----------------------------------------------------------------------------
# Apache Maven Wrapper startup batch script, version 3.3.4
#
# Optional ENV vars
# -----------------
#   JAVA_HOME - location of a JDK home dir, required when download maven via java source
#   MVNW_REPOURL - repo url base for downloading maven distribution
#   MVNW_USERNAME/MVNW_PASSWORD - user and password for downloading maven
#   MVNW_VERBOSE - true: enable verbose log; debug: trace the mvnw script; others: silence the output
# ----------------------------------------------------------------------------

set -euf
[ "${MVNW_VERBOSE-}" != debug ] || set -x

# OS specific support.
native_path() { printf %s\\n "$1"; }
case "$(uname)" in
CYGWIN* | MINGW*)
  [ -z "${JAVA_HOME-}" ] || JAVA_HOME="$(cygpath --unix "$JAVA_HOME")"
  native_path() { cygpath --path --windows "$1"; }
  ;;
esac

# set JAVACMD and JAVACCMD
set_java_home() {
  # For Cygwin and MinGW, ensure paths are in Unix format before anything is touched
  if [ -n "${JAVA_HOME-}" ]; then
    if [ -x "$JAVA_HOME/jre/sh/java" ]; then
      # IBM's JDK on AIX uses strange locations for the executables
      JAVACMD="$JAVA_HOME/jre/sh/java"
      JAVACCMD="$JAVA_HOME/jre/sh/javac"
    else
      JAVACMD="$JAVA_HOME/bin/java"
      JAVACCMD="$JAVA_HOME/bin/javac"

      if [ ! -x "$JAVACMD" ] || [ ! -x "$JAVACCMD" ]; then
        echo "The JAVA_HOME environment variable is not defined correctly, so mvnw cannot run." >&2
        echo "JAVA_HOME is set to \"$JAVA_HOME\", but \"\$JAVA_HOME/bin/java\" or \"\$JAVA_HOME/bin/javac\" does not exist." >&2
        return 1
      fi
    fi
  else
    JAVACMD="$(
      'set' +e
      'unset' -f command 2>/dev/null
      'command' -v java
    )" || :
    JAVACCMD="$(
      'set' +e
      'unset' -f command 2>/dev/null
      'command' -v javac
    )" || :

    if [ ! -x "${JAVACMD-}" ] || [ ! -x "${JAVACCMD-}" ]; then
      echo "The java/javac command does not exist in PATH nor is JAVA_HOME set, so mvnw cannot run." >&2
      return 1
    fi
  fi
}

# hash string like Java String::hashCode
hash_string() {
  str="${1:-}" h=0
  while [ -n "$str" ]; do
    char="${str%"${str#?}"}"
    h=$(((h * 31 + $(LC_CTYPE=C printf %d "'$char")) % 4294967296))
    str="${str#?}"
  done
  printf %x\\n $h
}

verbose() { :; }
[ "${MVNW_VERBOSE-}" != true ] || verbose() { printf %s\\n "${1-}"; }

die() {
  printf %s\\n "$1" >&2
  exit 1
}

trim() {
  # MWRAPPER-139:
  #   Trims trailing and leading whitespace, carriage returns, tabs, and linefeeds.
  #   Needed for removing poorly interpreted newline sequences when running in more
  #   exotic environments such as mingw bash on Windows.
  printf "%s" "${1}" | tr -d '[:space:]'
}

scriptDir="$(dirname "$0")"
scriptName="$(basename "$0")"

# parse distributionUrl and optional distributionSha256Sum, requires .mvn/wrapper/maven-wrapper.properties
while IFS="=" read -r key value; do
  case "${key-}" in
  distributionUrl) distributionUrl=$(trim "${value-}") ;;
  distributionSha256Sum) distributionSha256Sum=$(trim "${value-}") ;;
  esac
done <"$scriptDir/.mvn/wrapper/maven-wrapper.properties"
[ -n "${distributionUrl-}" ] || die "cannot read distributionUrl property in $scriptDir/.mvn/wrapper/maven-wrapper.properties"

case "${distributionUrl##*/}" in
maven-mvnd-*bin.*)
  MVN_CMD=mvnd.sh _MVNW_REPO_PATTERN=/maven/mvnd/
  case "${PROCESSOR_ARCHITECTURE-}${PROCESSOR_ARCHITEW6432-}:$(uname -a)" in
  *AMD64:CYGWIN* | *AMD64:MINGW*) distributionPlatform=windows-amd64 ;;
  :Darwin*x86_64) distributionPlatform=darwin-amd64 ;;
  :Darwin*arm64) distributionPlatform=darwin-aarch64 ;;
  :Linux*x86_64*) distributionPlatform=linux-amd64 ;;
  *)
    echo "Cannot detect native platform for mvnd on $(uname)-$(uname -m), use pure java version" >&2
    distributionPlatform=linux-amd64
    ;;
  esac
  distributionUrl="${distributionUrl%-bin.*}-$distributionPlatform.zip"
  ;;
maven-mvnd-*) MVN_CMD=mvnd.sh _MVNW_REPO_PATTERN=/maven/mvnd/ ;;
*) MVN_CMD="mvn${scriptName#mvnw}" _MVNW_REPO_PATTERN=/org/apache/maven/ ;;
esac

# apply MVNW_REPOURL and calculate MAVEN_HOME
# maven home pattern: ~/.m2/wrapper/dists/{apache-maven-<version>,maven-mvnd-<version>-<platform>}/<hash>
[ -z "${MVNW_REPOURL-}" ] || distributionUrl="$MVNW_REPOURL$_MVNW_REPO_PATTERN${distributionUrl#*"$_MVNW_REPO_PATTERN"}"
distributionUrlName="${distributionUrl##*/}"
distributionUrlNameMain="${distributionUrlName%.*}"
distributionUrlNameMain="${distributionUrlNameMain%-bin}"
MAVEN_USER_HOME="${MAVEN_USER_HOME:-${HOME}/.m2}"
MAVEN_HOME="${MAVEN_USER_HOME}/wrapper/dists/${distributionUrlNameMain-}/$(hash_string "$distributionUrl")"

exec_maven() {
  unset MVNW_VERBOSE MVNW_USERNAME MVNW_PASSWORD MVNW_REPOURL || :
  exec "$MAVEN_HOME/bin/$MVN_CMD" "$@" || die "cannot exec $MAVEN_HOME/bin/$MVN_CMD"
}

if [ -d "$MAVEN_HOME" ]; then
  verbose "found existing MAVEN_HOME at $MAVEN_HOME"
  exec_maven "$@"
fi

case "${distributionUrl-}" in
*?-bin.zip | *?maven-mvnd-?*-?*.zip) ;;
*) die "distributionUrl is not valid, must match *-bin.zip or maven-mvnd-*.zip, but found '${distributionUrl-}'" ;;
esac

# prepare tmp dir
if TMP_DOWNLOAD_DIR="$(mktemp -d)" && [ -d "$TMP_DOWNLOAD_DIR" ]; then
  clean() { rm -rf -- "$TMP_DOWNLOAD_DIR"; }
  trap clean HUP INT TERM EXIT
else
  die "cannot create temp dir"
fi

mkdir -p -- "${MAVEN_HOME%/*}"

# Download and Install Apache Maven
verbose "Couldn't find MAVEN_HOME, downloading and installing it ..."
verbose "Downloading from: $distributionUrl"
verbose "Downloading to: $TMP_DOWNLOAD_DIR/$distributionUrlName"

# select .zip or .tar.gz
if ! command -v unzip >/dev/null; then
  distributionUrl="${distributionUrl%.zip}.tar.gz"
  distributionUrlName="${distributionUrl##*/}"
fi

# verbose opt
__MVNW_QUIET_WGET=--quiet __MVNW_QUIET_CURL=--silent __MVNW_QUIET_UNZIP=-q __MVNW_QUIET_TAR=''
[ "${MVNW_VERBOSE-}" != true ] || __MVNW_QUIET_WGET='' __MVNW_QUIET_CURL='' __MVNW_QUIET_UNZIP='' __MVNW_QUIET_TAR=v

# normalize http auth
case "${MVNW_PASSWORD:+has-password}" in
'') MVNW_USERNAME='' MVNW_PASSWORD='' ;;
has-password) [ -n "${MVNW_USERNAME-}" ] || MVNW_USERNAME='' MVNW_PASSWORD='' ;;
esac

if [ -z "${MVNW_USERNAME-}" ] && command -v wget >/dev/null; then
  verbose "Found wget ... using wget"
  wget ${__MVNW_QUIET_WGET:+"$__MVNW_QUIET_WGET"} "$distributionUrl" -O "$TMP_DOWNLOAD_DIR/$distributionUrlName" || die "wget: Failed to fetch $distributionUrl"
elif [ -z "${MVNW_USERNAME-}" ] && command -v curl >/dev/null; then
  verbose "Found curl ... using curl"
  curl ${__MVNW_QUIET_CURL:+"$__MVNW_QUIET_CURL"} -f -L -o "$TMP_DOWNLOAD_DIR/$distributionUrlName" "$distributionUrl" || die "curl: Failed to fetch $distributionUrl"
elif set_java_home; then
  verbose "Falling back to use Java to download"
  javaSource="$TMP_DOWNLOAD_DIR/Downloader.java"
  targetZip="$TMP_DOWNLOAD_DIR/$distributionUrlName"
  cat >"$javaSource" <<-END
	public class Downloader extends java.net.Authenticator
	{
	  protected java.net.PasswordAuthentication getPasswordAuthentication()
	  {
	    return new java.net.PasswordAuthentication( System.getenv( "MVNW_USERNAME" ), System.getenv( "MVNW_PASSWORD" ).toCharArray() );
	  }
	  public static void main( String[] args ) throws Exception
	  {
	    setDefault( new Downloader() );
	    java.nio.file.Files.copy( java.net.URI.create( args[0] ).toURL().openStream(), java.nio.file.Paths.get( args[1] ).toAbsolutePath().normalize() );
	  }
	}
	END
  # For Cygwin/MinGW, switch paths to Windows format before running javac and java
  verbose " - Compiling Downloader.java ..."
  "$(native_path "$JAVACCMD")" "$(native_path "$javaSource")" || die "Failed to compile Downloader.java"
  verbose " - Running Downloader.java ..."
  "$(native_path "$JAVACMD")" -cp "$(native_path "$TMP_DOWNLOAD_DIR")" Downloader "$distributionUrl" "$(native_path "$targetZip")"
fi

# If specified, validate the SHA-256 sum of the Maven distribution zip file
if [ -n "${distributionSha256Sum-}" ]; then
  distributionSha256Result=false
  if [ "$MVN_CMD" = mvnd.sh ]; then
    echo "Checksum validation is not supported for maven-mvnd." >&2
    echo "Please disable validation by removing 'distributionSha256Sum' from your maven-wrapper.properties." >&2
    exit 1
  elif command -v sha256sum >/dev/null; then
    if echo "$distributionSha256Sum  $TMP_DOWNLOAD_DIR/$distributionUrlName" | sha256sum -c - >/dev/null 2>&1; then
      distributionSha256Result=true
    fi
  elif command -v shasum >/dev/null; then
    if echo "$distributionSha256Sum  $TMP_DOWNLOAD_DIR/$distributionUrlName" | shasum -a 256 -c >/dev/null 2>&1; then
      distributionSha256Result=true
    fi
  else
    echo "Checksum validation was requested but neither 'sha256sum' or 'shasum' are available." >&2
    echo "Please install either command, or disable validation by removing 'distributionSha256Sum' from your maven-wrapper.properties." >&2
    exit 1
  fi
  if [ $distributionSha256Result = false ]; then
    echo "Error: Failed to validate Maven distribution SHA-256, your Maven distribution might be compromised." >&2
    echo "If you updated your Maven version, you need to update the specified distributionSha256Sum property." >&2
    exit 1
  fi
fi

# unzip and move
if command -v unzip >/dev/null; then
  unzip ${__MVNW_QUIET_UNZIP:+"$__MVNW_QUIET_UNZIP"} "$TMP_DOWNLOAD_DIR/$distributionUrlName" -d "$TMP_DOWNLOAD_DIR" || die "failed to unzip"
else
  tar xzf${__MVNW_QUIET_TAR:+"$__MVNW_QUIET_TAR"} "$TMP_DOWNLOAD_DIR/$distributionUrlName" -C "$TMP_DOWNLOAD_DIR" || die "failed to untar"
fi

# Find the actual extracted directory name (handles snapshots where filename != directory name)
actualDistributionDir=""

# First try the expected directory name (for regular distributions)
if [ -d "$TMP_DOWNLOAD_DIR/$distributionUrlNameMain" ]; then
  if [ -f "$TMP_DOWNLOAD_DIR/$distributionUrlNameMain/bin/$MVN_CMD" ]; then
    actualDistributionDir="$distributionUrlNameMain"
  fi
fi

# If not found, search for any directory with the Maven executable (for snapshots)
if [ -z "$actualDistributionDir" ]; then
  # enable globbing to iterate over items
  set +f
  for dir in "$TMP_DOWNLOAD_DIR"/*; do
    if [ -d "$dir" ]; then
      if [ -f "$dir/bin/$MVN_CMD" ]; then
        actualDistributionDir="$(basename "$dir")"
        break
      fi
    fi
  done
  set -f
fi

if [ -z "$actualDistributionDir" ]; then
  verbose "Contents of $TMP_DOWNLOAD_DIR:"
  verbose "$(ls -la "$TMP_DOWNLOAD_DIR")"
  die "Could not find Maven distribution directory in extracted archive"
fi

verbose "Found extracted Maven distribution directory: $actualDistributionDir"
printf %s\\n "$distributionUrl" >"$TMP_DOWNLOAD_DIR/$actualDistributionDir/mvnw.url"
mv -- "$TMP_DOWNLOAD_DIR/$actualDistributionDir" "$MAVEN_HOME" || [ -d "$MAVEN_HOME" ] || die "fail to move MAVEN_HOME"

clean || :
exec_maven "$@"
````

## File: note-service/mvnw.cmd
````batch
<# : batch portion
@REM ----------------------------------------------------------------------------
@REM Licensed to the Apache Software Foundation (ASF) under one
@REM or more contributor license agreements.  See the NOTICE file
@REM distributed with this work for additional information
@REM regarding copyright ownership.  The ASF licenses this file
@REM to you under the Apache License, Version 2.0 (the
@REM "License"); you may not use this file except in compliance
@REM with the License.  You may obtain a copy of the License at
@REM
@REM    http://www.apache.org/licenses/LICENSE-2.0
@REM
@REM Unless required by applicable law or agreed to in writing,
@REM software distributed under the License is distributed on an
@REM "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
@REM KIND, either express or implied.  See the License for the
@REM specific language governing permissions and limitations
@REM under the License.
@REM ----------------------------------------------------------------------------

@REM ----------------------------------------------------------------------------
@REM Apache Maven Wrapper startup batch script, version 3.3.4
@REM
@REM Optional ENV vars
@REM   MVNW_REPOURL - repo url base for downloading maven distribution
@REM   MVNW_USERNAME/MVNW_PASSWORD - user and password for downloading maven
@REM   MVNW_VERBOSE - true: enable verbose log; others: silence the output
@REM ----------------------------------------------------------------------------

@IF "%__MVNW_ARG0_NAME__%"=="" (SET __MVNW_ARG0_NAME__=%~nx0)
@SET __MVNW_CMD__=
@SET __MVNW_ERROR__=
@SET __MVNW_PSMODULEP_SAVE=%PSModulePath%
@SET PSModulePath=
@FOR /F "usebackq tokens=1* delims==" %%A IN (`powershell -noprofile "& {$scriptDir='%~dp0'; $script='%__MVNW_ARG0_NAME__%'; icm -ScriptBlock ([Scriptblock]::Create((Get-Content -Raw '%~f0'))) -NoNewScope}"`) DO @(
  IF "%%A"=="MVN_CMD" (set __MVNW_CMD__=%%B) ELSE IF "%%B"=="" (echo %%A) ELSE (echo %%A=%%B)
)
@SET PSModulePath=%__MVNW_PSMODULEP_SAVE%
@SET __MVNW_PSMODULEP_SAVE=
@SET __MVNW_ARG0_NAME__=
@SET MVNW_USERNAME=
@SET MVNW_PASSWORD=
@IF NOT "%__MVNW_CMD__%"=="" ("%__MVNW_CMD__%" %*)
@echo Cannot start maven from wrapper >&2 && exit /b 1
@GOTO :EOF
: end batch / begin powershell #>

$ErrorActionPreference = "Stop"
if ($env:MVNW_VERBOSE -eq "true") {
  $VerbosePreference = "Continue"
}

# calculate distributionUrl, requires .mvn/wrapper/maven-wrapper.properties
$distributionUrl = (Get-Content -Raw "$scriptDir/.mvn/wrapper/maven-wrapper.properties" | ConvertFrom-StringData).distributionUrl
if (!$distributionUrl) {
  Write-Error "cannot read distributionUrl property in $scriptDir/.mvn/wrapper/maven-wrapper.properties"
}

switch -wildcard -casesensitive ( $($distributionUrl -replace '^.*/','') ) {
  "maven-mvnd-*" {
    $USE_MVND = $true
    $distributionUrl = $distributionUrl -replace '-bin\.[^.]*$',"-windows-amd64.zip"
    $MVN_CMD = "mvnd.cmd"
    break
  }
  default {
    $USE_MVND = $false
    $MVN_CMD = $script -replace '^mvnw','mvn'
    break
  }
}

# apply MVNW_REPOURL and calculate MAVEN_HOME
# maven home pattern: ~/.m2/wrapper/dists/{apache-maven-<version>,maven-mvnd-<version>-<platform>}/<hash>
if ($env:MVNW_REPOURL) {
  $MVNW_REPO_PATTERN = if ($USE_MVND -eq $False) { "/org/apache/maven/" } else { "/maven/mvnd/" }
  $distributionUrl = "$env:MVNW_REPOURL$MVNW_REPO_PATTERN$($distributionUrl -replace "^.*$MVNW_REPO_PATTERN",'')"
}
$distributionUrlName = $distributionUrl -replace '^.*/',''
$distributionUrlNameMain = $distributionUrlName -replace '\.[^.]*$','' -replace '-bin$',''

$MAVEN_M2_PATH = "$HOME/.m2"
if ($env:MAVEN_USER_HOME) {
  $MAVEN_M2_PATH = "$env:MAVEN_USER_HOME"
}

if (-not (Test-Path -Path $MAVEN_M2_PATH)) {
    New-Item -Path $MAVEN_M2_PATH -ItemType Directory | Out-Null
}

$MAVEN_WRAPPER_DISTS = $null
if ((Get-Item $MAVEN_M2_PATH).Target[0] -eq $null) {
  $MAVEN_WRAPPER_DISTS = "$MAVEN_M2_PATH/wrapper/dists"
} else {
  $MAVEN_WRAPPER_DISTS = (Get-Item $MAVEN_M2_PATH).Target[0] + "/wrapper/dists"
}

$MAVEN_HOME_PARENT = "$MAVEN_WRAPPER_DISTS/$distributionUrlNameMain"
$MAVEN_HOME_NAME = ([System.Security.Cryptography.SHA256]::Create().ComputeHash([byte[]][char[]]$distributionUrl) | ForEach-Object {$_.ToString("x2")}) -join ''
$MAVEN_HOME = "$MAVEN_HOME_PARENT/$MAVEN_HOME_NAME"

if (Test-Path -Path "$MAVEN_HOME" -PathType Container) {
  Write-Verbose "found existing MAVEN_HOME at $MAVEN_HOME"
  Write-Output "MVN_CMD=$MAVEN_HOME/bin/$MVN_CMD"
  exit $?
}

if (! $distributionUrlNameMain -or ($distributionUrlName -eq $distributionUrlNameMain)) {
  Write-Error "distributionUrl is not valid, must end with *-bin.zip, but found $distributionUrl"
}

# prepare tmp dir
$TMP_DOWNLOAD_DIR_HOLDER = New-TemporaryFile
$TMP_DOWNLOAD_DIR = New-Item -Itemtype Directory -Path "$TMP_DOWNLOAD_DIR_HOLDER.dir"
$TMP_DOWNLOAD_DIR_HOLDER.Delete() | Out-Null
trap {
  if ($TMP_DOWNLOAD_DIR.Exists) {
    try { Remove-Item $TMP_DOWNLOAD_DIR -Recurse -Force | Out-Null }
    catch { Write-Warning "Cannot remove $TMP_DOWNLOAD_DIR" }
  }
}

New-Item -Itemtype Directory -Path "$MAVEN_HOME_PARENT" -Force | Out-Null

# Download and Install Apache Maven
Write-Verbose "Couldn't find MAVEN_HOME, downloading and installing it ..."
Write-Verbose "Downloading from: $distributionUrl"
Write-Verbose "Downloading to: $TMP_DOWNLOAD_DIR/$distributionUrlName"

$webclient = New-Object System.Net.WebClient
if ($env:MVNW_USERNAME -and $env:MVNW_PASSWORD) {
  $webclient.Credentials = New-Object System.Net.NetworkCredential($env:MVNW_USERNAME, $env:MVNW_PASSWORD)
}
[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12
$webclient.DownloadFile($distributionUrl, "$TMP_DOWNLOAD_DIR/$distributionUrlName") | Out-Null

# If specified, validate the SHA-256 sum of the Maven distribution zip file
$distributionSha256Sum = (Get-Content -Raw "$scriptDir/.mvn/wrapper/maven-wrapper.properties" | ConvertFrom-StringData).distributionSha256Sum
if ($distributionSha256Sum) {
  if ($USE_MVND) {
    Write-Error "Checksum validation is not supported for maven-mvnd. `nPlease disable validation by removing 'distributionSha256Sum' from your maven-wrapper.properties."
  }
  Import-Module $PSHOME\Modules\Microsoft.PowerShell.Utility -Function Get-FileHash
  if ((Get-FileHash "$TMP_DOWNLOAD_DIR/$distributionUrlName" -Algorithm SHA256).Hash.ToLower() -ne $distributionSha256Sum) {
    Write-Error "Error: Failed to validate Maven distribution SHA-256, your Maven distribution might be compromised. If you updated your Maven version, you need to update the specified distributionSha256Sum property."
  }
}

# unzip and move
Expand-Archive "$TMP_DOWNLOAD_DIR/$distributionUrlName" -DestinationPath "$TMP_DOWNLOAD_DIR" | Out-Null

# Find the actual extracted directory name (handles snapshots where filename != directory name)
$actualDistributionDir = ""

# First try the expected directory name (for regular distributions)
$expectedPath = Join-Path "$TMP_DOWNLOAD_DIR" "$distributionUrlNameMain"
$expectedMvnPath = Join-Path "$expectedPath" "bin/$MVN_CMD"
if ((Test-Path -Path $expectedPath -PathType Container) -and (Test-Path -Path $expectedMvnPath -PathType Leaf)) {
  $actualDistributionDir = $distributionUrlNameMain
}

# If not found, search for any directory with the Maven executable (for snapshots)
if (!$actualDistributionDir) {
  Get-ChildItem -Path "$TMP_DOWNLOAD_DIR" -Directory | ForEach-Object {
    $testPath = Join-Path $_.FullName "bin/$MVN_CMD"
    if (Test-Path -Path $testPath -PathType Leaf) {
      $actualDistributionDir = $_.Name
    }
  }
}

if (!$actualDistributionDir) {
  Write-Error "Could not find Maven distribution directory in extracted archive"
}

Write-Verbose "Found extracted Maven distribution directory: $actualDistributionDir"
Rename-Item -Path "$TMP_DOWNLOAD_DIR/$actualDistributionDir" -NewName $MAVEN_HOME_NAME | Out-Null
try {
  Move-Item -Path "$TMP_DOWNLOAD_DIR/$MAVEN_HOME_NAME" -Destination $MAVEN_HOME_PARENT | Out-Null
} catch {
  if (! (Test-Path -Path "$MAVEN_HOME" -PathType Container)) {
    Write-Error "fail to move MAVEN_HOME"
  }
} finally {
  try { Remove-Item $TMP_DOWNLOAD_DIR -Recurse -Force | Out-Null }
  catch { Write-Warning "Cannot remove $TMP_DOWNLOAD_DIR" }
}

Write-Output "MVN_CMD=$MAVEN_HOME/bin/$MVN_CMD"
````

## File: note-service/src/main/java/com/note/service/common/config/JwtAuthenticationFilter.java
````java
//day03
package com.note.service.common.config;

import com.note.service.common.util.JwtUtils;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;

@Slf4j
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    private JwtUtils jwtUtils;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        // 获取请求头中的 Authorization
        String header = request.getHeader("Authorization");
        String token = null;

        // 提取 Token（Bearer token）
        if (header != null && header.startsWith("Bearer ")) {
            token = header.substring(7);
        }

        // 验证 Token
        if (token != null && jwtUtils.validateToken(token)) {
            Long userId = jwtUtils.getUserIdFromToken(token);
            String username = jwtUtils.getUsernameFromToken(token);

            // 将认证信息存入 SecurityContext（后续可用）
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(userId, null, new ArrayList<>());
            authentication.setDetails(username);
            SecurityContextHolder.getContext().setAuthentication(authentication);

            log.debug("JWT 验证通过: userId={}, username={}", userId, username);
        }

        filterChain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // 登录和注册接口不需要 JWT 验证
        String path = request.getRequestURI();
        return path.equals("/api/ums/login")
                || path.equals("/api/ums/register")
                || path.startsWith("/doc.html")        // Knife4j 页面
                || path.startsWith("/webjars/")
                || path.startsWith("/v3/api-docs/");
    }
}
````

## File: note-service/src/main/java/com/note/service/common/config/MybatisPlusConfig.java
````java
package com.note.service.common.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MybatisPlusConfig {

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        // 添加分页插件（指定数据库类型为 MySQL）
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        return interceptor;
    }
}
````

## File: note-service/src/main/java/com/note/service/common/config/RedisConfig.java
````java
package com.note.service.common.config;

//import com.fasterxml.jackson.databind.ObjectMapper;
//import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
//import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

//@Configuration
//public class RedisConfig {
//
//    @Bean
//    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory) {
//        RedisTemplate<String, Object> template = new RedisTemplate<>();
//        template.setConnectionFactory(factory);
//
//        // Key 用 String 序列化
//        template.setKeySerializer(new StringRedisSerializer());
//        template.setHashKeySerializer(new StringRedisSerializer());
//
//        // Value 用 JSON 序列化（带类型信息，便于反序列化）
//        ObjectMapper mapper = new ObjectMapper();
//        mapper.activateDefaultTyping(
//                LaissezFaireSubTypeValidator.instance,
//                ObjectMapper.DefaultTyping.NON_FINAL
//        );
//        GenericJackson2JsonRedisSerializer serializer =
//                new GenericJackson2JsonRedisSerializer(mapper);
//
//        template.setValueSerializer(serializer);
//        template.setHashValueSerializer(serializer);
//        template.afterPropertiesSet();
//
//        return template;
//    }
//}
@Configuration
public class RedisConfig {

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);

        // Key 用 String 序列化
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());

        // Value 只序列化字符串，避免反序列化漏洞
        StringRedisSerializer stringRedisSerializer = new StringRedisSerializer();
        template.setValueSerializer(stringRedisSerializer);
        template.setHashValueSerializer(stringRedisSerializer);

        template.afterPropertiesSet();
        return template;
    }
}
````

## File: note-service/src/main/java/com/note/service/common/config/SecurityConfig.java
````java
//day03
package com.note.service.common.config;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;  // 添加这行！
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@RequiredArgsConstructor
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // ========== 1. 启用 CORS ==========
                // 这一行告诉 Spring Security："跨域配置交给 CorsFilter 处理，你不要插手"
                // 必须加，否则 Security 会拦截 OPTIONS 请求
                .cors(Customizer.withDefaults())

                // 禁用 CSRF（前后端分离不需要）
                .csrf(AbstractHttpConfigurer::disable)

                // 无状态会话（不用 Session）
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // 配置授权规则
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/ums/register", "/api/ums/login").permitAll()
                        .requestMatchers("/doc.html", "/webjars/**", "/swagger-resources/**", "/v3/api-docs/**").permitAll()
                        .anyRequest().authenticated()
                )
                // 添加 JWT 过滤器
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
````

## File: note-service/src/main/java/com/note/service/common/exception/BusinessException.java
````java
//day03
package com.note.service.common.exception;

import lombok.Getter;

//业务异常处理
@Getter
public class BusinessException extends RuntimeException {
    private final Integer code;

    public BusinessException(String message) {
        super(message);
        this.code = 500;
    }

    public BusinessException(Integer code, String message) {
        super(message);
        this.code = code;
    }
}
````

## File: note-service/src/main/java/com/note/service/common/exception/GlobalExceptionHandler.java
````java
//day03
package com.note.service.common.exception;

import com.note.service.common.vo.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

//全局异常处理
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // 处理参数校验异常（@Valid 失败）
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<String> handleValidationException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getAllErrors().get(0).getDefaultMessage();
        log.warn("参数校验失败: {}", message);
        return Result.error(400, message);
    }

    // 处理业务异常（自定义）
    @ExceptionHandler(BusinessException.class)
    public Result<String> handleBusinessException(BusinessException e) {
        log.warn("业务异常: {}", e.getMessage());
        return Result.error(e.getCode(), e.getMessage());
    }

    // 处理其他所有异常
    @ExceptionHandler(Exception.class)
    public Result<String> handleException(Exception e) {
        log.error("系统异常", e);
        return Result.error("系统繁忙，请稍后重试");
    }
}
````

## File: note-service/src/main/java/com/note/service/common/util/JwtUtils.java
````java
//day03
package com.note.service.common.util;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtUtils {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private Long expiration;

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    // 生成 Token（存储 userId 和 username）
    public String generateToken(Long userId, String username) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration);

        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("username", username)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(getSigningKey(), Jwts.SIG.HS256)
                .compact();
    }

    // 从 Token 获取用户ID
    public Long getUserIdFromToken(String token) {
        Claims claims = parseToken(token);
        return Long.valueOf(claims.getSubject());
    }

    // 从 Token 获取用户名
    public String getUsernameFromToken(String token) {
        Claims claims = parseToken(token);
        return claims.get("username", String.class);
    }

    // 验证 Token 是否有效
    public boolean validateToken(String token) {
        try {
            parseToken(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    private Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
````

## File: note-service/src/main/java/com/note/service/common/vo/Result.java
````java
//day03
package com.note.service.common.vo;

import lombok.Data;

//建立统一响应类
@Data
public class Result<T> {
    private Integer code;
    private String message;
    private T data;

    public static <T> Result<T> success(T data) {
        Result<T> result = new Result<>();
        result.setCode(200);
        result.setMessage("success");
        result.setData(data);
        return result;
    }

    public static <T> Result<T> success() {
        return success(null);
    }

    public static <T> Result<T> error(String message) {
        Result<T> result = new Result<>();
        result.setCode(500);
        result.setMessage(message);
        return result;
    }

    public static <T> Result<T> error(Integer code, String message) {
        Result<T> result = new Result<>();
        result.setCode(code);
        result.setMessage(message);
        return result;
    }
}
````

## File: note-service/src/main/java/com/note/service/controller/NoteController.java
````java
package com.note.service.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.note.service.common.vo.Result;
import com.note.service.dto.NoteDTO;
import com.note.service.dto.NoteQueryDTO;
import com.note.service.entity.NoteEntity;
import com.note.service.service.NoteService;
import com.note.service.mapper.NoteTagMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import lombok.extern.slf4j.Slf4j;  // 如果用 Lombok
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@RestController
@RequestMapping("/api/note")
@RequiredArgsConstructor
@Validated
@Tag(name = "笔记管理", description = "笔记的增删改查与标签管理")
public class NoteController {


    private final NoteService noteService;
    private final NoteTagMapper noteTagMapper;          // 用于 /tags 接口
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;            // Spring Boot 自动注册，直接注入即可

    @PostMapping
    @Operation(summary = "创建笔记")
    public Result<Long> create(@RequestBody @Valid NoteDTO dto,
                               @AuthenticationPrincipal Long userId) {
        Long noteId = noteService.createNote(userId, dto);
        return Result.success(noteId);
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取笔记详情")
    public Result<NoteEntity> detail(@PathVariable Long id,
                                     @AuthenticationPrincipal Long userId) {
        return Result.success(noteService.getDetail(id, userId));
    }


    @PutMapping("/{id}")
    @Operation(summary = "更新笔记")
    public Result<Void> update(@PathVariable Long id,
                               @RequestBody @Valid NoteDTO dto,
                               @AuthenticationPrincipal Long userId) {
        noteService.updateNote(id, userId, dto);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除笔记（默认软删除，permanent=true 物理删除）")
    public Result<Void> delete(@PathVariable Long id,
                               @RequestParam(defaultValue = "false") boolean permanent,
                               @AuthenticationPrincipal Long userId) {
        noteService.deleteNote(id, userId, permanent);
        return Result.success();
    }

    @GetMapping("/page")
    @Operation(summary = "分页查询笔记列表")
    public Result<Page<NoteEntity>> page(NoteQueryDTO query,
                                         @AuthenticationPrincipal Long userId) {
        return Result.success(noteService.pageQuery(userId, query));
    }

    // ==================== 回收站接口 ====================

    @GetMapping("/recycle/page")
    @Operation(summary = "获取回收站列表")
    public Result<Page<NoteEntity>> recyclePage(NoteQueryDTO query,
                                                @AuthenticationPrincipal Long userId) {
        query.setIsDeleted(1);
        return Result.success(noteService.pageRecycle(userId, query));
    }

    @PutMapping("/{id}/restore")
    @Operation(summary = "从回收站恢复笔记")
    public Result<Void> restore(@PathVariable Long id,
                                @AuthenticationPrincipal Long userId) {
        noteService.restoreFromRecycle(id, userId);
        return Result.success();
    }

    @DeleteMapping("/recycle/clear")
    @Operation(summary = "清空回收站")
    public Result<Void> clearRecycle(@AuthenticationPrincipal Long userId) {
        noteService.clearRecycle(userId);
        return Result.success();
    }

    // ==================== 标签接口 ====================

    @GetMapping("/tags")
    @Operation(summary = "获取当前用户所有标签")
    public Result<List<String>> getUserTags(@AuthenticationPrincipal Long userId) {
        List<String> tags = noteTagMapper.selectDistinctTagsByUserId(userId);
        return Result.success(tags);
    }

    // ==================== 草稿接口（Jackson 版）====================

    /**
     * 构建草稿 Redis Key
     * 新建笔记草稿：note:draft:{userId}:new
     * 已有笔记草稿：note:draft:{userId}:{noteId}
     */
    private String buildDraftKey(Long userId, Long noteId) {
        if (noteId != null) {
            return "note:draft:" + userId + ":" + noteId;
        }
        // 新建笔记用固定后缀 "new"，避免多 tab 歧义时 key 混乱
        // 已知限制：同用户同时新建多篇时会覆盖，可接受（面试可讲）
        return "note:draft:" + userId + ":new";
    }

    @PostMapping("/draft")
    @Operation(summary = "自动保存草稿（3秒防抖后前端触发）")
    public Result<Void> saveDraft(@RequestBody NoteDTO dto,
                                  @AuthenticationPrincipal Long userId) {
        String key = buildDraftKey(userId, dto.getId());
        try {
            String json = objectMapper.writeValueAsString(dto);
            stringRedisTemplate.opsForValue().set(key, json, 7, TimeUnit.DAYS);
            log.debug("草稿已保存: key={}", key);
        } catch (JsonProcessingException e) {
            // 序列化失败不影响主流程，只记录日志
            log.error("草稿序列化失败: userId={}, noteId={}", userId, dto.getId(), e);
        }
        return Result.success();
    }

    @GetMapping("/draft")
    @Operation(summary = "获取草稿（进入编辑页时查询）")
    public Result<NoteDTO> getDraft(@RequestParam(required = false) Long noteId,
                                    @AuthenticationPrincipal Long userId) {
        String key = buildDraftKey(userId, noteId);
        String json = stringRedisTemplate.opsForValue().get(key);

        if (json == null) {
            return Result.success(null);  // 无草稿，返回 null，前端正常处理
        }

        try {
            NoteDTO draft = objectMapper.readValue(json, NoteDTO.class);
            log.debug("草稿已读取: key={}", key);
            return Result.success(draft);
        } catch (JsonProcessingException e) {
            // 草稿数据损坏，删除并返回 null
            log.error("草稿反序列化失败，已清除: key={}", key, e);
            stringRedisTemplate.delete(key);
            return Result.success(null);
        }
    }

    @DeleteMapping("/draft")
    @Operation(summary = "清除草稿（保存成功后前端调用）")
    public Result<Void> clearDraft(@RequestParam(required = false) Long noteId,
                                   @AuthenticationPrincipal Long userId) {
        String key = buildDraftKey(userId, noteId);
        Boolean deleted = stringRedisTemplate.delete(key);
        log.debug("草稿已清除: key={}, existed={}", key, deleted);
        return Result.success();
    }
}
````

## File: note-service/src/main/java/com/note/service/dto/LoginDTO.java
````java
//day03
package com.note.service.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginDTO {
    @NotBlank(message = "用户名不能为空")
    private String username;

    @NotBlank(message = "密码不能为空")
    private String password;
}
````

## File: note-service/src/main/java/com/note/service/dto/NoteDTO.java
````java
package com.note.service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import java.util.List;

@Data
public class NoteDTO {

    private Long id;  // 更新时用

    @NotBlank(message = "标题不能为空")
    @Size(max = 200, message = "标题最长200字符")
    private String title;

    private String content;  // Markdown原文

    @Size(max = 10, message = "最多10个标签")
    private List<String> tags;
}
````

## File: note-service/src/main/java/com/note/service/dto/NoteQueryDTO.java
````java
package com.note.service.dto;

import lombok.Data;
//day05-添加标签筛选字段
import java.util.List;

@Data
public class NoteQueryDTO {
    private Integer pageNum = 1;
    private Integer pageSize = 20;
    private String keyword;      // 标题搜索（暂不实现内容搜索）
    private Integer isDeleted = 0;
    private List<String> tags;  //day05-标签筛选-新增
    private String tagMatch="ANY";  //ANY-任意标签 ALL-全部标签-新增
}
````

## File: note-service/src/main/java/com/note/service/entity/NoteEntity.java
````java
package com.note.service.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Arrays;
import java.util.ArrayList;

@Data
@TableName("note")
public class NoteEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;
    private String title;
    private String content;
    private String summary;

    @TableField("is_deleted")
    private Integer isDeleted;

    private LocalDateTime deletedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;


    // 非数据库字段，用于接收 GROUP_CONCAT 结果
    @TableField(exist = false)
    private String tagNames;

    @TableField(exist = false)
    private List<String> tags;

    // 解析 tagNames 到 tags 列表的方法
    public void parseTagNames() {
        if (this.tagNames != null && !this.tagNames.isEmpty()) {
            this.tags = Arrays.asList(this.tagNames.split(","));
        } else {
            this.tags = new ArrayList<>();
        }
    }
}
````

## File: note-service/src/main/java/com/note/service/entity/NoteTagEntity.java
````java
package com.note.service.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("note_tag")
public class NoteTagEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long noteId;
    private String tagName;
    private LocalDateTime createdAt;
}
````

## File: note-service/src/main/java/com/note/service/mapper/NoteMapper.java
````java
package com.note.service.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.note.service.entity.NoteEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface NoteMapper extends BaseMapper<NoteEntity> {

    // 分页查询用户的笔记（带标签聚合）
    // 这个是原有的分页查询方法，在day05步骤二更改为 一下
//    @Select("SELECT n.*, GROUP_CONCAT(nt.tag_name) as tagNames " +
//            "FROM note n LEFT JOIN note_tag nt ON n.id = nt.note_id " +
//            "WHERE n.user_id = #{userId} AND n.is_deleted = #{isDeleted} " +
//            "GROUP BY n.id ORDER BY n.updated_at DESC")
//    Page<NoteEntity> selectPageWithTags(Page<NoteEntity> page,
//                                        @Param("userId") Long userId,
//                                        @Param("isDeleted") Integer isDeleted);

     //查询当前页数据（带标签筛选）- 手动分页
     //
    @Select("<script>" +
            "SELECT n.*, GROUP_CONCAT(nt.tag_name) as tagNames " +
            "FROM note n LEFT JOIN note_tag nt ON n.id = nt.note_id " +
            "WHERE n.user_id = #{userId} AND n.is_deleted = #{isDeleted} " +
            "<if test='keyword != null and keyword != \"\"'>" +
            " AND n.title LIKE CONCAT('%', #{keyword}, '%')" +
            "</if>" +
            "<if test='tags != null and tags.size() > 0'>" +
            " AND n.id IN (" +
            "   SELECT note_id FROM note_tag WHERE tag_name IN " +
            "   <foreach collection='tags' item='tag' open='(' separator=',' close=')'>#{tag}</foreach> " +
            "   GROUP BY note_id " +
            "   <if test='tagMatch == \"ALL\"'>" +
            "   HAVING COUNT(DISTINCT tag_name) = #{tags.size}" +
            "   </if>" +
            " )" +
            "</if>" +
            "GROUP BY n.id " +
            "ORDER BY n.updated_at DESC " +
            "LIMIT #{offset}, #{limit}" +
            "</script>")
    List<NoteEntity> selectWithTags(@Param("userId") Long userId,
                                    @Param("isDeleted") Integer isDeleted,
                                    @Param("keyword") String keyword,
                                    @Param("tags") List<String> tags,
                                    @Param("tagMatch") String tagMatch,
                                    @Param("offset") long offset,
                                    @Param("limit") long limit);

    /**
     * 查询总记录数（带标签筛选）- 使用 COUNT DISTINCT 确保准确
     */
    @Select("<script>" +
            "SELECT COUNT(DISTINCT n.id) FROM note n " +
            "LEFT JOIN note_tag nt ON n.id = nt.note_id " +
            "WHERE n.user_id = #{userId} AND n.is_deleted = #{isDeleted} " +
            "<if test='keyword != null and keyword != \"\"'>" +
            " AND n.title LIKE CONCAT('%', #{keyword}, '%')" +
            "</if>" +
            "<if test='tags != null and tags.size() > 0'>" +
            " AND n.id IN (" +
            "   SELECT note_id FROM note_tag WHERE tag_name IN " +
            "   <foreach collection='tags' item='tag' open='(' separator=',' close=')'>#{tag}</foreach> " +
            "   GROUP BY note_id " +
            "   <if test='tagMatch == \"ALL\"'>" +
            "   HAVING COUNT(DISTINCT tag_name) = #{tags.size}" +
            "   </if>" +
            " )" +
            "</if>" +
            "</script>")
    long countWithTags(@Param("userId") Long userId,
                       @Param("isDeleted") Integer isDeleted,
                       @Param("keyword") String keyword,
                       @Param("tags") List<String> tags,
                       @Param("tagMatch") String tagMatch);

}
````

## File: note-service/src/main/java/com/note/service/mapper/NoteTagMapper.java
````java
package com.note.service.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.note.service.entity.NoteTagEntity;
import com.note.service.entity.NoteEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import java.util.List;

@Mapper
public interface NoteTagMapper extends BaseMapper<NoteTagEntity> {


    @Select("SELECT tag_name FROM note_tag WHERE note_id = #{noteId}")
    List<String> selectTagsByNoteId(@Param("noteId") Long noteId);
    // 根据用户ID查询所有不重复的标签（Day 5 步骤 14 需要的）- 这个可能没添加
    @Select("SELECT DISTINCT tag_name FROM note_tag WHERE note_id IN " +
            "(SELECT id FROM note WHERE user_id = #{userId})")
    List<String> selectDistinctTagsByUserId(@Param("userId") Long userId);

    //day05-添加标签筛选的分页查询
    @Select("<script>" +
            "SELECT n.*, GROUP_CONCAT(nt.tag_name) as tagNames " +
            "FROM note n LEFT JOIN note_tag nt ON n.id = nt.note_id " +
            "WHERE n.user_id = #{userId} AND n.is_deleted = #{isDeleted} " +
            "<if test='keyword != null and keyword != \"\"'>" +
            " AND n.title LIKE CONCAT('%', #{keyword}, '%')" +
            "</if>" +
            "<if test='tags != null and tags.size() > 0'>" +
            " AND n.id IN (" +
            "   SELECT note_id FROM note_tag WHERE tag_name IN " +
            "   <foreach collection='tags' item='tag' open='(' separator=',' close=')'>#{tag}</foreach> " +
            "   GROUP BY note_id " +
            "   <if test='tagMatch == \"ALL\"'>" +
            "   HAVING COUNT(DISTINCT tag_name) = #{tags.size}" +
            "   </if>" +
            " )" +
            "</if>" +
            "GROUP BY n.id ORDER BY n.updated_at DESC" +
            "</script>")
    Page<NoteEntity> selectPageWithTags(@Param("page") Page<NoteEntity> page,
                                        @Param("userId") Long userId,
                                        @Param("isDeleted") Integer isDeleted,
                                        @Param("keyword") String keyword,
                                        @Param("tags") List<String> tags,
                                        @Param("tagMatch") String tagMatch);
}
````

## File: note-service/src/main/java/com/note/service/NoteServiceApplication.java
````java
package com.note.service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class NoteServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(NoteServiceApplication.class, args);
	}

}
````

## File: note-service/src/main/java/com/note/service/service/NoteService.java
````java
package com.note.service.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.note.service.common.exception.BusinessException;
import com.note.service.dto.NoteDTO;
import com.note.service.dto.NoteQueryDTO;
import com.note.service.entity.NoteEntity;
import com.note.service.entity.NoteTagEntity;
import com.note.service.mapper.NoteMapper;
import com.note.service.mapper.NoteTagMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.ArrayList;

@Slf4j
@Service
@RequiredArgsConstructor
public class NoteService extends ServiceImpl<NoteMapper, NoteEntity> {

    private final NoteTagMapper noteTagMapper;

    // ========== 核心 CRUD ==========

    @Transactional
    public Long createNote(Long userId, NoteDTO dto) {
        // 1. 保存笔记主表
        NoteEntity note = new NoteEntity();
        note.setUserId(userId);
        note.setTitle(dto.getTitle().trim());
        note.setContent(dto.getContent());
        note.setSummary(generateSummary(dto.getContent()));
        note.setIsDeleted(0);

        baseMapper.insert(note);

        // 2. 保存标签（去重、小写、 trim）
        if (dto.getTags() != null && !dto.getTags().isEmpty()) {
            saveTags(note.getId(), dto.getTags());
        }

        log.info("创建笔记成功: userId={}, noteId={}", userId, note.getId());
        return note.getId();
    }

    public NoteEntity getDetail(Long noteId, Long userId) {
        NoteEntity note = baseMapper.selectById(noteId);
        if (note == null || !note.getUserId().equals(userId)) {
            throw new BusinessException(404, "笔记不存在或无权限");
        }
        if (note.getIsDeleted() == 1) {
            throw new BusinessException(404, "笔记已在回收站");
        }

        // 组装标签
        List<String> tags = noteTagMapper.selectTagsByNoteId(noteId);
        note.setTags(tags);

        return note;
    }

    @Transactional
    public void updateNote(Long noteId, Long userId, NoteDTO dto) {
        // 1. 校验权限
        NoteEntity exist = getDetail(noteId, userId);  // 会抛异常如果无权限

        // 2. 更新主表
        exist.setTitle(dto.getTitle().trim());
        exist.setContent(dto.getContent());
        exist.setSummary(generateSummary(dto.getContent()));
        baseMapper.updateById(exist);

        // 3. 更新标签：先删后插（简单策略）
        noteTagMapper.delete(new QueryWrapper<NoteTagEntity>().eq("note_id", noteId));
        if (dto.getTags() != null && !dto.getTags().isEmpty()) {
            saveTags(noteId, dto.getTags());
        }

        log.info("更新笔记成功: noteId={}", noteId);
    }

    @Transactional
    public void deleteNote(Long noteId, Long userId, boolean permanent) {
        NoteEntity note = baseMapper.selectById(noteId);
        if (note == null || !note.getUserId().equals(userId)) {
            throw new BusinessException(403, "无权操作");
        }

        if (permanent) {
            // 物理删除
            baseMapper.deleteById(noteId);
            noteTagMapper.delete(new QueryWrapper<NoteTagEntity>().eq("note_id", noteId));
            log.info("物理删除笔记: noteId={}", noteId);
        } else {
            // 软删除（移入回收站）
            note.setIsDeleted(1);
            note.setDeletedAt(LocalDateTime.now());
            baseMapper.updateById(note);
            log.info("移入回收站: noteId={}", noteId);
        }
    }

    // ========== 分页查询 ==========

    /**
     * 分页查询笔记列表（支持标签筛选）
     */
    public Page<NoteEntity> pageQuery(Long userId, NoteQueryDTO query) {
        // 如果没有标签筛选，走原来的简单查询（性能更好）
        if (query.getTags() == null || query.getTags().isEmpty()) {
            return simplePageQuery(userId, query);
        }

        // 有标签筛选：使用手动分页
        return tagFilterPageQuery(userId, query);
    }

    /**
     * 简单分页查询（无标签筛选）- 复用原有逻辑
     */
    private Page<NoteEntity> simplePageQuery(Long userId, NoteQueryDTO query) {
        Page<NoteEntity> page = new Page<>(query.getPageNum(), query.getPageSize());

        QueryWrapper<NoteEntity> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId)
                .eq("is_deleted", query.getIsDeleted());

        if (StringUtils.hasText(query.getKeyword())) {
            wrapper.like("title", query.getKeyword());
        }

        wrapper.orderByDesc("updated_at");
        page = baseMapper.selectPage(page, wrapper);

        // 组装标签
        if (page.getRecords() != null) {
            page.getRecords().forEach(note -> {
                List<String> tags = noteTagMapper.selectTagsByNoteId(note.getId());
                note.setTags(tags);
            });
        }

        return page;
    }

    /**
     * 标签筛选分页查询（手动分页解决 COUNT 问题）
     */
    private Page<NoteEntity> tagFilterPageQuery(Long userId, NoteQueryDTO query) {
        // 计算分页参数
        long offset = (long) (query.getPageNum() - 1) * query.getPageSize();
        long limit = query.getPageSize();

        // 1. 查询当前页的数据
        List<NoteEntity> records = baseMapper.selectWithTags(
                userId,
                query.getIsDeleted(),
                query.getKeyword(),
                query.getTags(),
                query.getTagMatch(),
                offset,
                limit
        );

        // 2. 解析 tagNames 到 tags 列表
        if (records != null) {
            records.forEach(NoteEntity::parseTagNames);
        }

        // 3. 查询总记录数
        long total = baseMapper.countWithTags(
                userId,
                query.getIsDeleted(),
                query.getKeyword(),
                query.getTags(),
                query.getTagMatch()
        );

        // 4. 手动组装 Page 对象
        Page<NoteEntity> page = new Page<>(query.getPageNum(), query.getPageSize(), total);
        page.setRecords(records != null ? records : new ArrayList<>());

        return page;
    }
//    // 这是原有的分页查询方法（day05步骤二指出有错误，替换为以下新的）
//    public Page<NoteEntity> pageQuery(Long userId, NoteQueryDTO query) {
//        Page<NoteEntity> page = new Page<>(query.getPageNum(), query.getPageSize());
//
//        // 简单实现：先查主表，再组装标签（避免复杂 SQL）
//        QueryWrapper<NoteEntity> wrapper = new QueryWrapper<>();
//        wrapper.eq("user_id", userId)
//                .eq("is_deleted", query.getIsDeleted());
//
//        // 关键词搜索（仅标题）
//        if (StringUtils.hasText(query.getKeyword())) {
//            wrapper.like("title", query.getKeyword());
//        }
//
//        wrapper.orderByDesc("updated_at");
//        page = baseMapper.selectPage(page, wrapper);
//
//        // 批量组装标签（可优化为批量查询，当前逐条）
//        page.getRecords().forEach(note -> {
//            List<String> tags = noteTagMapper.selectTagsByNoteId(note.getId());
//            note.setTags(tags);
//        });
//
//        return page;
//    }

    // ========== 工具方法 ==========

    private void saveTags(Long noteId, List<String> tags) {
        // 清洗：去重、小写、trim、过滤空串
        List<String> cleanedTags = tags.stream()
                .map(String::trim)
                .filter(StringUtils::hasText)
                .map(String::toLowerCase)
                .distinct()
                .limit(10)  // 最多10个
                .collect(Collectors.toList());

        for (String tagName : cleanedTags) {
            NoteTagEntity tag = new NoteTagEntity();
            tag.setNoteId(noteId);
            tag.setTagName(tagName);
            noteTagMapper.insert(tag);
        }
    }

    private String generateSummary(String content) {
        if (!StringUtils.hasText(content)) return "";
        // 简单清洗 Markdown 标记，取前200字符
        String plain = content
                .replaceAll("#", "")
                .replaceAll("\\*", "")
                .replaceAll("`", "")
                .replaceAll("\\[", "")
                .replaceAll("\\]", "")
                .replaceAll("\\(", "")
                .replaceAll("\\)", "")
                .replaceAll("\n", " ")
                .trim();

        return plain.length() > 200 ? plain.substring(0, 200) + "..." : plain;
    }

    // ========== 回收站相关 ==========

    /**
     * 从回收站恢复笔记
     */
    @Transactional
    public void restoreFromRecycle(Long noteId, Long userId) {
        NoteEntity note = baseMapper.selectById(noteId);
        if (note == null || !note.getUserId().equals(userId)) {
            throw new BusinessException(403, "无权操作");
        }
        if (note.getIsDeleted() != 1) {
            throw new BusinessException(400, "笔记不在回收站");
        }

        note.setIsDeleted(0);
        note.setDeletedAt(null);
        baseMapper.updateById(note);
        log.info("恢复笔记: noteId={}", noteId);
    }

    /**
     * 清空回收站（物理删除所有回收站笔记）
     */
    @Transactional
    public void clearRecycle(Long userId) {
        QueryWrapper<NoteEntity> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId).eq("is_deleted", 1);

        List<NoteEntity> recycleNotes = baseMapper.selectList(wrapper);
        for (NoteEntity note : recycleNotes) {
            // 物理删除笔记（同时删除标签）
            baseMapper.deleteById(note.getId());
            noteTagMapper.delete(new QueryWrapper<NoteTagEntity>().eq("note_id", note.getId()));
        }
        log.info("清空回收站: userId={}, count={}", userId, recycleNotes.size());
    }

    /**
     * 获取回收站列表
     */
    public Page<NoteEntity> pageRecycle(Long userId, NoteQueryDTO query) {
        Page<NoteEntity> page = new Page<>(query.getPageNum(), query.getPageSize());

        QueryWrapper<NoteEntity> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId)
                .eq("is_deleted", 1)
                .orderByDesc("deleted_at");  // 按删除时间倒序

        if (StringUtils.hasText(query.getKeyword())) {
            wrapper.like("title", query.getKeyword());
        }

        page = baseMapper.selectPage(page, wrapper);

        // 组装标签
        page.getRecords().forEach(note -> {
            List<String> tags = noteTagMapper.selectTagsByNoteId(note.getId());
            note.setTags(tags);
        });

        return page;
    }
}
````

## File: note-service/src/main/resources/db/migration/V2__create_note_and_tag.sql
````sql
CREATE TABLE IF NOT EXISTS `note` (
                                      `id`            BIGINT AUTO_INCREMENT PRIMARY KEY,
                                      `user_id`       BIGINT       NOT NULL COMMENT '作者ID',
                                      `title`         VARCHAR(200) NOT NULL DEFAULT '未命名笔记',
    `content`       LONGTEXT     COMMENT 'Markdown内容',
    `summary`       VARCHAR(500) COMMENT '纯文本摘要',
    `is_deleted`    TINYINT      NOT NULL DEFAULT 0 COMMENT '0-正常 1-回收站',
    `deleted_at`    DATETIME     COMMENT '删除时间',
    `created_at`    DATETIME     DEFAULT CURRENT_TIMESTAMP,
    `updated_at`    DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX `idx_user_id` (`user_id`),
    INDEX `idx_is_deleted` (`is_deleted`),
    INDEX `idx_updated_at` (`updated_at`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='笔记表';

-- 标签关联表（简化版，不单独建标签字典表）

CREATE TABLE IF NOT EXISTS `note_tag` (
                                          `id`            BIGINT AUTO_INCREMENT PRIMARY KEY,
                                          `note_id`       BIGINT       NOT NULL,
                                          `tag_name`      VARCHAR(50)  NOT NULL COMMENT '标签名',
    `created_at`    DATETIME     DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY `uk_note_tag` (`note_id`, `tag_name`),
    INDEX `idx_tag_name` (`tag_name`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='笔记标签关联表';
````

## File: note-service/src/test/java/com/note/service/NoteServiceApplicationTests.java
````java
package com.note.service;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class NoteServiceApplicationTests {

	@Test
	void contextLoads() {
	}

}
````

## File: note-ui/.gitignore
````
# Logs
logs
*.log
npm-debug.log*
yarn-debug.log*
yarn-error.log*
pnpm-debug.log*
lerna-debug.log*

node_modules
dist
dist-ssr
*.local

# Editor directories and files
.vscode/*
!.vscode/extensions.json
.idea
.DS_Store
*.suo
*.ntvs*
*.njsproj
*.sln
*.sw?
````

## File: note-ui/public/vite.svg
````xml
<svg xmlns="http://www.w3.org/2000/svg" xmlns:xlink="http://www.w3.org/1999/xlink" aria-hidden="true" role="img" class="iconify iconify--logos" width="31.88" height="32" preserveAspectRatio="xMidYMid meet" viewBox="0 0 256 257"><defs><linearGradient id="IconifyId1813088fe1fbc01fb466" x1="-.828%" x2="57.636%" y1="7.652%" y2="78.411%"><stop offset="0%" stop-color="#41D1FF"></stop><stop offset="100%" stop-color="#BD34FE"></stop></linearGradient><linearGradient id="IconifyId1813088fe1fbc01fb467" x1="43.376%" x2="50.316%" y1="2.242%" y2="89.03%"><stop offset="0%" stop-color="#FFEA83"></stop><stop offset="8.333%" stop-color="#FFDD35"></stop><stop offset="100%" stop-color="#FFA800"></stop></linearGradient></defs><path fill="url(#IconifyId1813088fe1fbc01fb466)" d="M255.153 37.938L134.897 252.976c-2.483 4.44-8.862 4.466-11.382.048L.875 37.958c-2.746-4.814 1.371-10.646 6.827-9.67l120.385 21.517a6.537 6.537 0 0 0 2.322-.004l117.867-21.483c5.438-.991 9.574 4.796 6.877 9.62Z"></path><path fill="url(#IconifyId1813088fe1fbc01fb467)" d="M185.432.063L96.44 17.501a3.268 3.268 0 0 0-2.634 3.014l-5.474 92.456a3.268 3.268 0 0 0 3.997 3.378l24.777-5.718c2.318-.535 4.413 1.507 3.936 3.838l-7.361 36.047c-.495 2.426 1.782 4.5 4.151 3.78l15.304-4.649c2.372-.72 4.652 1.36 4.15 3.788l-11.698 56.621c-.732 3.542 3.979 5.473 5.943 2.437l1.313-2.028l72.516-144.72c1.215-2.423-.88-5.186-3.54-4.672l-25.505 4.922c-2.396.462-4.435-1.77-3.759-4.114l16.646-57.705c.677-2.35-1.37-4.583-3.769-4.113Z"></path></svg>
````

## File: note-ui/src/App.vue
````vue
<template>
  <router-view />
</template>
````

## File: note-ui/src/components/MarkdownEditor.vue
````vue
<template>
  <div class="md-editor">
    <div class="toolbar">
      <el-button-group>
        <el-button size="small" @click="insert('**', '**')">B</el-button>
        <el-button size="small" @click="insert('*', '*')">I</el-button>
        <el-button size="small" @click="insert('## ', '')">H</el-button>
        <el-button size="small" @click="insert('- ', '')">List</el-button>
        <el-button size="small" @click="insert('```\n', '\n```')">Code</el-button>
      </el-button-group>

      <el-button type="primary" size="small" :loading="saving" @click="save">
        保存
      </el-button>
    </div>

    <div class="editor-container">
      <textarea
          ref="textarea"
          v-model="content"
          class="edit-area"
          placeholder="输入 Markdown..."
          @input="onInput"
      />
      <div class="preview-area" v-html="renderedHtml" />
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { marked } from 'marked'
import hljs from 'highlight.js'
import 'highlight.js/styles/github.css'

const props = defineProps({
  modelValue: { type: String, default: '' },
  saving: { type: Boolean, default: false }
})

const emit = defineEmits(['update:modelValue', 'save'])

const content = ref(props.modelValue)
const textarea = ref(null)

// 配置 marked
marked.setOptions({
  highlight: (code, lang) => {
    if (lang && hljs.getLanguage(lang)) {
      return hljs.highlight(code, { language: lang }).value
    }
    return hljs.highlightAuto(code).value
  },
  breaks: true,
  gfm: true
})

const renderedHtml = computed(() => marked.parse(content.value))

watch(() => props.modelValue, (val) => {
  if (val !== content.value) content.value = val
})

const onInput = () => {
  emit('update:modelValue', content.value)
}

const insert = (before, after) => {
  const el = textarea.value
  const start = el.selectionStart
  const end = el.selectionEnd
  const selected = content.value.substring(start, end)

  content.value = (
      content.value.substring(0, start) +
      before + selected + after +
      content.value.substring(end)
  )

  emit('update:modelValue', content.value)

  // 恢复光标
  setTimeout(() => {
    el.focus()
    const newPos = start + before.length + selected.length
    el.setSelectionRange(newPos, newPos)
  }, 0)
}

const save = () => {
  emit('save')
}
</script>

<style scoped>
.md-editor {
  height: calc(100vh - 200px);
  display: flex;
  flex-direction: column;
  background: #fff;
  border-radius: 4px;
}

.toolbar {
  padding: 10px;
  border-bottom: 1px solid #dcdfe6;
  display: flex;
  justify-content: space-between;
}

.editor-container {
  flex: 1;
  display: flex;
  overflow: hidden;
}

.edit-area, .preview-area {
  flex: 1;
  padding: 20px;
  overflow-y: auto;
  font-family: 'Monaco', 'Menlo', 'Consolas', monospace;
  font-size: 14px;
  line-height: 1.6;
}

.edit-area {
  border: none;
  border-right: 1px solid #dcdfe6;
  resize: none;
  outline: none;
  background: #fafafa;
}

.preview-area :deep(h1) { font-size: 2em; border-bottom: 1px solid #eaecef; padding-bottom: 0.3em; }
.preview-area :deep(h2) { font-size: 1.5em; border-bottom: 1px solid #eaecef; padding-bottom: 0.3em; }
.preview-area :deep(pre) { background: #f6f8fa; padding: 16px; border-radius: 6px; overflow-x: auto; }
.preview-area :deep(code) { font-family: 'SFMono-Regular', Consolas, monospace; font-size: 85%; }
.preview-area :deep(blockquote) { border-left: 4px solid #dfe2e5; padding-left: 16px; color: #6a737d; margin: 0; }
</style>
````

## File: note-ui/src/counter.ts
````typescript
export function setupCounter(element: HTMLButtonElement) {
  let counter = 0
  const setCounter = (count: number) => {
    counter = count
    element.innerHTML = `count is ${counter}`
  }
  element.addEventListener('click', () => setCounter(counter + 1))
  setCounter(0)
}
````

## File: note-ui/src/main.js
````javascript
import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'

import App from './App.vue'
import router from './router'

const app = createApp(App)

// 注册所有图标
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
    app.component(key, component)
}

app.use(createPinia())
app.use(router)
app.use(ElementPlus)

app.mount('#app')
````

## File: note-ui/src/router/index.js
````javascript
import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/store/user'

const routes = [
    {
        path: '/login',
        name: 'Login',
        component: () => import('@/views/Login.vue'),
        meta: { public: true }
    },
    {
        path: '/register',
        name: 'Register',
        component: () => import('@/views/Register.vue'),
        meta: { public: true }
    },
    {
        path: '/',
        component: () => import('@/views/Layout.vue'),
        meta: { requiresAuth: true },
        children: [
            {
                path: '',
                name: 'NoteList',
                component: () => import('@/views/NoteList.vue')
            },
            {
                //day05更新路由配置：添加回收站到Layout的子路由中
                path:'recycle',
                name:'RecycleBin',
                component: ()=> import('@/views/RecycleBin.vue')
            },
            {
                path: 'note/new',
                name: 'NoteCreate',
                component: () => import('@/views/NoteEdit.vue')
            },
            {
                path: 'note/:id',
                name: 'NoteEdit',
                component: () => import('@/views/NoteEdit.vue')
            }
        ]
    }
]

const router = createRouter({
    history: createWebHistory(),
    routes
})

// 全局路由守卫
router.beforeEach((to, from, next) => {
    const userStore = useUserStore()

    if (to.meta.requiresAuth && !userStore.token) {
        next('/login')
    } else if (to.meta.public && userStore.token) {
        // 已登录用户访问登录页，跳首页
        next('/')
    } else {
        next()
    }
})

export default router
````

## File: note-ui/src/store/user.js
````javascript
import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

export const useUserStore = defineStore('user', () => {
    // State
    const token = ref(localStorage.getItem('token') || '')
    // const userInfo = ref(null)
    const userInfo = ref(JSON.parse(localStorage.getItem('userInfo') || 'null'))

    // Getters
    const isLoggedIn = computed(() => !!token.value)

    // Actions
    const setToken = (newToken) => {
        token.value = newToken
        localStorage.setItem('token', newToken)
    }

    const setUserInfo = (info) => {
        userInfo.value = info
        localStorage.setItem('userInfo',JSON.stringify(info))//持久化
    }

    const logout = () => {
        token.value = ''
        userInfo.value = null
        localStorage.removeItem('token')
        localStorage.removeItem('userInfo')
    }

    return {
        token,
        userInfo,
        isLoggedIn,
        setToken,
        setUserInfo,
        logout
    }
})
````

## File: note-ui/src/style.css
````css
:root {
  font-family: system-ui, Avenir, Helvetica, Arial, sans-serif;
  line-height: 1.5;
  font-weight: 400;

  color-scheme: light dark;
  color: rgba(255, 255, 255, 0.87);
  background-color: #242424;

  font-synthesis: none;
  text-rendering: optimizeLegibility;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
}

a {
  font-weight: 500;
  color: #646cff;
  text-decoration: inherit;
}
a:hover {
  color: #535bf2;
}

body {
  margin: 0;
  display: flex;
  place-items: center;
  min-width: 320px;
  min-height: 100vh;
}

h1 {
  font-size: 3.2em;
  line-height: 1.1;
}

#app {
  max-width: 1280px;
  margin: 0 auto;
  padding: 2rem;
  text-align: center;
}

.logo {
  height: 6em;
  padding: 1.5em;
  will-change: filter;
  transition: filter 300ms;
}
.logo:hover {
  filter: drop-shadow(0 0 2em #646cffaa);
}
.logo.vanilla:hover {
  filter: drop-shadow(0 0 2em #3178c6aa);
}

.card {
  padding: 2em;
}

.read-the-docs {
  color: #888;
}

button {
  border-radius: 8px;
  border: 1px solid transparent;
  padding: 0.6em 1.2em;
  font-size: 1em;
  font-weight: 500;
  font-family: inherit;
  background-color: #1a1a1a;
  cursor: pointer;
  transition: border-color 0.25s;
}
button:hover {
  border-color: #646cff;
}
button:focus,
button:focus-visible {
  outline: 4px auto -webkit-focus-ring-color;
}

@media (prefers-color-scheme: light) {
  :root {
    color: #213547;
    background-color: #ffffff;
  }
  a:hover {
    color: #747bff;
  }
  button {
    background-color: #f9f9f9;
  }
}
````

## File: note-ui/src/typescript.svg
````xml
<svg xmlns="http://www.w3.org/2000/svg" xmlns:xlink="http://www.w3.org/1999/xlink" aria-hidden="true" role="img" class="iconify iconify--logos" width="32" height="32" preserveAspectRatio="xMidYMid meet" viewBox="0 0 256 256"><path fill="#007ACC" d="M0 128v128h256V0H0z"></path><path fill="#FFF" d="m56.612 128.85l-.081 10.483h33.32v94.68h23.568v-94.68h33.321v-10.28c0-5.69-.122-10.444-.284-10.566c-.122-.162-20.4-.244-44.983-.203l-44.74.122l-.121 10.443Zm149.955-10.742c6.501 1.625 11.459 4.51 16.01 9.224c2.357 2.52 5.851 7.111 6.136 8.208c.08.325-11.053 7.802-17.798 11.988c-.244.162-1.22-.894-2.317-2.52c-3.291-4.795-6.745-6.867-12.028-7.233c-7.76-.528-12.759 3.535-12.718 10.321c0 1.992.284 3.17 1.097 4.795c1.707 3.536 4.876 5.649 14.832 9.956c18.326 7.883 26.168 13.084 31.045 20.48c5.445 8.249 6.664 21.415 2.966 31.208c-4.063 10.646-14.14 17.879-28.323 20.276c-4.388.772-14.79.65-19.504-.203c-10.28-1.828-20.033-6.908-26.047-13.572c-2.357-2.6-6.949-9.387-6.664-9.874c.122-.163 1.178-.813 2.356-1.504c1.138-.65 5.446-3.129 9.509-5.485l7.355-4.267l1.544 2.276c2.154 3.29 6.867 7.801 9.712 9.305c8.167 4.307 19.383 3.698 24.909-1.26c2.357-2.153 3.332-4.388 3.332-7.68c0-2.966-.366-4.266-1.91-6.501c-1.99-2.845-6.054-5.242-17.595-10.24c-13.206-5.69-18.895-9.224-24.096-14.832c-3.007-3.25-5.852-8.452-7.03-12.8c-.975-3.617-1.22-12.678-.447-16.335c2.723-12.76 12.353-21.659 26.25-24.3c4.51-.853 14.994-.528 19.424.569Z"></path></svg>
````

## File: note-ui/src/utils/request.js
````javascript
import axios from 'axios'
import { useUserStore } from '@/store/user'
import { ElMessage } from 'element-plus'
import router from '@/router'

const request = axios.create({
    baseURL: 'http://localhost:8080/api',
    timeout: 10000,
    headers: {
        'Content-Type': 'application/json'
    }
})

// 请求拦截器：自动添加 Token
request.interceptors.request.use(
    (config) => {
        const userStore = useUserStore()
        if (userStore.token) {
            config.headers.Authorization = `Bearer ${userStore.token}`
        }
        return config
    },
    (error) => {
        return Promise.reject(error)
    }
)

// 响应拦截器：统一错误处理
request.interceptors.response.use(
    (response) => {
        const res = response.data
        // 你的 Result 包装格式：{ code, message, data }
        // 成功响应（200）
        if (res.code === 200) {
            return res.data//只返回data部分
        }
        // 业务错误（400，401，404）
        const message=res.message || '请求失败'
        ElMessage.error(message)

        // 401令牌过期
        if (res.code === 401){
            const userStore=useUserStore()
            userStore.logout()
            router.push("/login")
        }

        return Promise.reject({
            code:res.code,
            message:message,
            data:res.data
        })
    },
    (error) => {
        const { response } = error
        if (!response) {
            // 网络完全断开，只弹一次
            ElMessage.error({ message: '网络连接失败，请检查后端服务', grouping: true })
            return Promise.reject(error)
        }

        if (response?.status === 401 || response?.status=== 403) {
            ElMessage.error('登录已过期，请重新登录')
            const userStore = useUserStore()
            userStore.logout()
            router.push('/login')
        } else {
            ElMessage.error({ message: response?.data?.message || '请求失败', grouping: true })
        }

        return Promise.reject(error)
    }
)

export default request
````

## File: note-ui/src/views/Layout.vue
````vue
<template>
  <el-container class="layout">
    <el-header class="header">
      <div class="logo">Note-lite</div>
      <div class="user-info">
        <span>{{ userStore.userInfo?.username }}</span>
        <el-button type="text" @click="logout">退出</el-button>
      </div>
    </el-header>

    <el-container>
      <el-aside width="200px" class="aside">
        <el-menu
            :router="true"
            :default-active="$route.path"
            class="menu"
        >
          <el-menu-item index="/">
            <el-icon><Document /></el-icon>
            <span>我的笔记</span>
          </el-menu-item>
          <el-menu-item index="/recycle">  <!-- day05新增 -->
            <el-icon><Delete /></el-icon>
            <span>回收站</span>
          </el-menu-item>
          <el-menu-item index="/note/new">
            <el-icon><Plus /></el-icon>
            <span>新建笔记</span>
          </el-menu-item>
        </el-menu>
      </el-aside>

      <el-main class="main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { useUserStore } from '@/store/user'
import { useRouter } from 'vue-router'
import { Document, Plus , Delete} from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'


const userStore = useUserStore()
const router = useRouter()

const logout = () => {
  userStore.logout()
  ElMessage.success('已退出登录')
  router.push('/login')
}
</script>

<style scoped>
.layout {
  height: 100vh;
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: #fff;
  border-bottom: 1px solid #dcdfe6;
}

.logo {
  font-size: 20px;
  font-weight: bold;
  color: #409eff;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 15px;
}

.aside {
  background: #fff;
  border-right: 1px solid #dcdfe6;
}

.menu {
  border-right: none;
}

.main {
  background: #f5f7fa;
  padding: 20px;
}
</style>
````

## File: note-ui/src/views/Login.vue
````vue
<template>
  <div class="login-container">
    <el-card class="login-box">
      <h2 class="title">Note-lite 登录</h2>

      <el-form
          ref="formRef"
          :model="form"
          :rules="rules"
          @keyup.enter="handleLogin"
      >
        <el-form-item prop="username">
          <el-input
              v-model="form.username"
              placeholder="用户名"
              :prefix-icon="User"
              size="large"
          />
        </el-form-item>

        <el-form-item prop="password">
          <el-input
              v-model="form.password"
              type="password"
              placeholder="密码"
              :prefix-icon="Lock"
              size="large"
              show-password
          />
        </el-form-item>

        <el-button
            type="primary"
            size="large"
            :loading="loading"
            @click="handleLogin"
            style="width: 100%"
        >
          登 录
        </el-button>

        <div class="actions">
          <router-link to="/register">注册新账号</router-link>
        </div>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { User, Lock } from '@element-plus/icons-vue'
import { useUserStore } from '@/store/user'
import request from '@/utils/request'
import { ElMessage } from 'element-plus'

const router = useRouter()
const userStore = useUserStore()
const formRef = ref()
const loading = ref(false)

const form = reactive({
  username: '',
  password: ''
})

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

const handleLogin = async () => {
  try {
    await formRef.value.validate()
    loading.value = true

    const res = await request.post('/ums/login', {
      username: form.username,
      password: form.password
    })

    // 假设登录返回：{ token, userId, username }
    userStore.setToken(res.token)
    userStore.setUserInfo({
      userId: res.userId,
      username: res.username
    })

    ElMessage.success('登录成功')
    router.push('/')
  } catch (error) {
    // 错误已在拦截器提示
    console.error('登录失败:', error)
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-container {
  height: 100vh;
  display: flex;
  justify-content: center;
  align-items: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.login-box {
  width: 400px;
  padding: 20px;
}

.title {
  text-align: center;
  margin-bottom: 30px;
  color: #333;
}

.actions {
  margin-top: 20px;
  text-align: center;
}
</style>
````

## File: note-ui/src/views/NoteEdit.vue
````vue
<template>
  <div class="note-edit">
    <el-input
        v-model="title"
        placeholder="输入标题..."
        class="title-input"
        size="large"
    />

    <MarkdownEditor
        v-model="content"
        :saving="saving"
        @save="handleSave"
    />
  </div>
</template>

<script setup>
import { ref, onMounted , onUnmounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'//day05总修改导入
import request from '@/utils/request'
import MarkdownEditor from '@/components/MarkdownEditor.vue'
import { debounce } from 'lodash-es'

const route = useRoute()
const router = useRouter()

const noteId = ref(route.params.id ? Number(route.params.id) : null)
const title = ref('')
const content = ref('')
const saving = ref(false)

// 加载已有笔记
onMounted(async () => {
  if (noteId.value) {
    try {
      const res = await request.get(`/note/${noteId.value}`)
      title.value = res.title
      content.value = res.content
    } catch (error) {
      ElMessage.error('加载笔记失败')
      router.push('/')
    }
  }
  //加载草稿
  await loadDraft()
})

const handleSave = async () => {
  if (!title.value.trim()) {
    ElMessage.warning('请输入标题')
    return
  }

  saving.value = true
  try {
    const payload = {
      title: title.value,
      content: content.value,
      tags: []  // 暂不实现标签编辑
    }

    let res
    if (noteId.value) {
      await request.put(`/note/${noteId.value}`, payload)
      ElMessage.success('保存成功')
    } else {
      const res = await request.post('/note', payload)
      noteId.value = res
      ElMessage.success('创建成功')
      // 更新 URL，避免重复创建
      await router.replace(`/note/${noteId.value}`)
    }
    //保存成功后清除草稿
    await clearDraft()
    // ✅ 跳转到列表页，并添加 refresh 参数强制刷新
    router.push({ path: '/', query: { refresh: Date.now() } })
  } catch (error) {
    // 错误已在拦截器处理
  } finally {
    saving.value = false
  }
}

// 自动保存定时器
let autoSaveTimer = null
const autoSave = debounce(async () => {
  if (!title.value && !content.value) return

  try {
    const payload = {
      id: noteId.value,
      title: title.value,
      content: content.value,
      tags: []
    }

    await request.post('/note/draft', payload)
    console.log('草稿已自动保存')
  } catch (error) {
    console.error('自动保存失败:', error)
  }
}, 3000)  // 3秒防抖

// 监听内容变化
watch([title, content], () => {
  autoSave()
}, { deep: true })

// 加载草稿
const loadDraft = async () => {
  try {
    const params = noteId.value ? { noteId: noteId.value } : {}
    const res = await request.get('/note/draft', { params })

    if (res) {
      // 如果有草稿，询问是否恢复
      ElMessageBox.confirm('检测到未保存的草稿，是否恢复？', '提示', {
        confirmButtonText: '恢复',
        cancelButtonText: '丢弃',
        type: 'info'
      }).then(() => {
        title.value = res.title || ''
        content.value = res.content || ''
      }).catch(() => {
        // 丢弃草稿
        clearDraft()
      })
    }
  } catch (error) {
    console.error('加载草稿失败:', error)
  }
}

// 清除草稿
const clearDraft = async () => {
  try {
    const params = noteId.value ? { noteId: noteId.value } : {}
    await request.delete('/note/draft', { params })
  } catch (error) {
    console.error('清除草稿失败:', error)
  }
}

// 组件卸载时清除定时器
onUnmounted(() => {
  // if (autoSaveTimer) {
  //   clearTimeout(autoSaveTimer)
  // }
  autoSave.cancel()
})

</script>

<style scoped>
.note-edit {
  max-width: 1400px;
  margin: 0 auto;
}

.title-input {
  margin-bottom: 20px;
}

.title-input :deep(.el-input__inner) {
  font-size: 24px;
  font-weight: bold;
  border: none;
  border-bottom: 2px solid #dcdfe6;
  border-radius: 0;
  padding: 10px 0;
}

.title-input :deep(.el-input__inner:focus) {
  border-bottom-color: #409eff;
}
</style>
````

## File: note-ui/src/views/NoteList.vue
````vue
<template>
  <div class="note-list">
    <div class="header">
      <h3>我的笔记</h3>
      <div class="header-right">
        <!-- ✅ 添加搜索框 -->
        <el-input
            v-model="keyword"
            placeholder="搜索笔记..."
            style="width: 200px"
            @keyup.enter="loadNotes"
        />
        <TagFilter
            v-model="selectedTags"
            v-model:match-mode="tagMatch"
            :all-tags="allTags"
            @change="handleTagFilter"
        />
        </div>
      <el-button type="primary" @click="createNote">
        <el-icon><Plus /></el-icon> 新建笔记
      </el-button>
    </div>

    <el-empty v-if="!loading && notes.length === 0" description="暂无笔记，点击右上角创建" />

<!--    <div v-else class="list">-->
<!--      <el-card-->
<!--          v-for="note in notes"-->
<!--          :key="note.id"-->
<!--          class="note-card"-->
<!--          shadow="hover"-->
<!--          @click="editNote(note.id)"-->
<!--      >-->
<!--        <h4 class="title">{{ note.title }}</h4>-->
<!--        <p class="summary">{{ note.summary || '暂无摘要' }}</p>-->
<!--        <div class="meta">-->
<!--          <el-tag v-for="tag in note.tags" :key="tag" size="small" class="tag">-->
<!--            {{ tag }}-->
<!--          </el-tag>-->
<!--          <span class="time">{{ formatTime(note.updatedAt) }}</span>-->
<!--        </div>-->
<!--      </el-card>-->
<!--    </div>-->
    <div v-else class="list">
      <el-card
          v-for="note in notes"
          :key="note.id"
          class="note-card"
          shadow="hover"
      >
        <div @click="editNote(note.id)" class="card-content">
          <h4 class="title">{{ note.title }}</h4>
          <p class="summary">{{ note.summary || '暂无摘要' }}</p>
          <div class="meta">
            <el-tag v-for="tag in note.tags" :key="tag" size="small" class="tag">
              {{ tag }}
            </el-tag>
            <span class="time">{{ formatTime(note.updatedAt) }}</span>
          </div>
        </div>
        <div class="card-actions">
          <el-button
              type="danger"
              size="small"
              :icon="Delete"
              circle
              @click.stop="softDelete(note.id)"
          />
        </div>
      </el-card>
    </div>

    <el-pagination
        v-if="total > 0"
        v-model:current-page="pageNum"
        :page-size="pageSize"
        :total="total"
        layout="prev, pager, next"
        @current-change="loadNotes"
    />
  </div>
</template>

<script setup>
import { ref, onMounted, onActivated} from 'vue'
import { useRouter, useRoute, onBeforeRouteUpdate} from 'vue-router'
import { Plus } from '@element-plus/icons-vue'
import request from '@/utils/request'
import TagFilter from '@/components/TagFilter.vue'//day05新增
import { Delete } from '@element-plus/icons-vue'//day05新增笔记删除方法
import { ElMessage, ElMessageBox } from 'element-plus'//day05总修改导入

const router = useRouter()
const route = useRoute()
const notes = ref([])
const loading = ref(false)
const pageNum = ref(1)
const pageSize = ref(20)
const total = ref(0)
const selectedTags = ref([])//day05新增
const tagMatch = ref('ANY')
const allTags = ref([])
const keyword=ref('')

// const loadNotes = async () => {
//   loading.value = true
//   try {
//     const res = await request.get('/note/page', {
//       params: {
//         pageNum: pageNum.value,
//         pageSize: pageSize.value
//       }
//     })
//     notes.value = res.data.records
//     total.value = res.data.total
//   } finally {
//     loading.value = false
//   }
// }
// 修改 loadNotes 方法
const loadNotes = async () => {
  loading.value = true
  try {
    const params = {
      pageNum: pageNum.value,
      pageSize: pageSize.value
    }

    if (selectedTags.value.length > 0) {
      params.tags = selectedTags.value
      params.tagMatch = tagMatch.value
    }

    if (keyword.value) {
      params.keyword = keyword.value
    }

    const res = await request.get('/note/page', { params })

    notes.value = res.records
    total.value = res.total
  } finally {
    loading.value = false
  }
}

const createNote = () => {
  router.push('/note/new')
}

const editNote = (id) => {
  router.push(`/note/${id}`)
}

const formatTime = (time) => {
  if (!time) return ''
  return new Date(time).toLocaleString('zh-CN', {
    month: 'short',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit'
  })
}

// 加载所有标签（用于筛选下拉）
const loadAllTags = async () => {
  try {
    const res = await request.get('/note/tags')
    allTags.value = res
  } catch (error) {
    console.error('加载标签失败:', error)
  }
}

// 处理标签筛选
const handleTagFilter = () => {
  pageNum.value = 1
  loadNotes()
}

// 方案 A：使用 onActivated（缓存组件时的最佳实践）
// ✅ 当从其他路由返回到这个组件时调用
onActivated(() => {
  // 检查 query 参数是否需要刷新
  if (route.query.refresh) {
    loadNotes()
  }
})

onMounted(()=>{
  loadNotes()
  loadAllTags()
})

//添加笔记删除方法
const softDelete = async (id) => {
  try {
    await ElMessageBox.confirm('确定将笔记移入回收站吗？', '提示', {
      type: 'warning'
    })
    await request.delete(`/note/${id}`)  // 默认软删除
    ElMessage.success('已移入回收站')
    loadNotes()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除失败:', error)
    }
  }
}

</script>

<style scoped>
.note-list {
  max-width: 1200px;
  margin: 0 auto;
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.list {
  display: grid;
  gap: 15px;
}

.note-card {
  cursor: pointer;
  transition: transform 0.2s;
  position: relative;
}

.card-content {
  padding-right: 40px;  /* 为删除按钮留出空间 */
}

.note-card:hover {
  transform: translateY(-2px);
}

.card-actions {
  position: absolute;
  top: 20px;
  right: 20px;
  opacity: 0;
  transition: opacity 0.2s;
}

.note-card:hover .card-actions {
  opacity: 1;
}

.title {
  margin: 0 0 10px 0;
  color: #303133;
}

.summary {
  color: #606266;
  font-size: 14px;
  margin-bottom: 10px;
  line-height: 1.5;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.meta {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.tag {
  margin-right: 5px;
}

.time {
  color: #909399;
  font-size: 12px;
  margin-left: auto;
}
</style>
````

## File: note-ui/src/views/Register.vue
````vue
<!-- src/views/Register.vue 新增不在day4中-->
<template>
  <div class="register-container">
    <el-card class="register-card">
      <h2>用户注册</h2>
      <el-form :model="registerForm" :rules="rules" ref="formRef">
        <el-form-item prop="username">
          <el-input v-model="registerForm.username" placeholder="用户名" />
        </el-form-item>
        <el-form-item prop="email">
          <el-input v-model="registerForm.email" placeholder="邮箱" />
        </el-form-item>
        <el-form-item prop="password">
          <el-input v-model="registerForm.password" type="password" placeholder="密码" />
        </el-form-item>
        <el-form-item prop="confirmPassword">
          <el-input v-model="registerForm.confirmPassword" type="password" placeholder="确认密码" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleRegister" style="width: 100%">注册</el-button>
        </el-form-item>
        <el-form-item>
          <el-link type="primary" @click="$router.push('/login')">已有账号？去登录</el-link>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'

const router = useRouter()
const formRef = ref(null)

const registerForm = reactive({
  username: '',
  email:'',//新增邮箱校验
  password: '',
  confirmPassword: ''
})

const validatePass2 = (rule, value, callback) => {
  if (value === '') {
    callback(new Error('请再次输入密码'))
  } else if (value !== registerForm.password) {
    callback(new Error('两次输入密码不一致!'))
  } else {
    callback()
  }
}

const rules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 20, message: '长度在 3 到 20 个字符', trigger: 'blur' }
  ],
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '请输入正确的邮箱格式', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码长度不能小于6位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入密码', trigger: 'blur' },
    { validator: validatePass2, trigger: 'blur' }
  ]
}

// const handleRegister = async () => {
//   if (!formRef.value) return
//
//   await formRef.value.validate((valid) => {
//     if (valid) {
//       // TODO: 调用注册接口
//       ElMessage.success('注册成功')
//       router.push('/login')
//     }
//   })
// }

import request from '@/utils/request'
//注册接口的完善
const handleRegister = async () => {
  if (!formRef.value) return

  await formRef.value.validate(async (valid) => {
    if (valid) {
      try {
        const res = await request.post('/ums/register', {
          username: registerForm.username,
          email: registerForm.email,
          password: registerForm.password
        })
        ElMessage.success('注册成功，请登录')
        router.push('/login')
      } catch (error) {
        // 错误已在拦截器处理
        console.error('注册失败:', error)
      }
    }
  })
}

</script>

<style scoped>
.register-container {
  height: 100vh;
  display: flex;
  justify-content: center;
  align-items: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.register-card {
  width: 400px;
}
</style>
````

## File: note-ui/tsconfig.json
````json
{
  "compilerOptions": {
    "target": "ES2022",
    "useDefineForClassFields": true,
    "module": "ESNext",
    "lib": ["ES2022", "DOM", "DOM.Iterable"],
    "types": ["vite/client"],
    "skipLibCheck": true,

    /* Bundler mode */
    "moduleResolution": "bundler",
    "allowImportingTsExtensions": true,
    "verbatimModuleSyntax": true,
    "moduleDetection": "force",
    "noEmit": true,

    /* Linting */
    "strict": true,
    "noUnusedLocals": true,
    "noUnusedParameters": true,
    "erasableSyntaxOnly": true,
    "noFallthroughCasesInSwitch": true,
    "noUncheckedSideEffectImports": true
  },
  "include": ["src"]
}
````

## File: note-ui/vite.config.js
````javascript
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import path from 'path'

export default defineConfig({
    plugins: [vue()],
    resolve: {
        alias: {
            '@': path.resolve(__dirname, './src')
        }
    },
    server: {
        port: 5173,
        open: true
    }
})
````

## File: README.md
````markdown
# Note-lite
````

## File: note-service/src/main/java/com/note/service/config/SwaggerConfig.java
````java
package com.note.service.config;

//day02
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {  // 类名可以不改，或改为 Knife4jConfig

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Note Service API")
                        .version("1.0")
                        .description("笔记服务接口文档")
                        .contact(new Contact()
                                .name("作者")
                                .email("your.email@example.com")));
    }
}
````

## File: note-service/src/main/java/com/note/service/controller/UserController.java
````java
//day02
//package com.note.service.controller;
//
//
//import com.note.service.dto.UserRegisterDTO;
//import com.note.service.service.UserService;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.*;
//@RestController
//@RequestMapping("/api/ums")
//public class UserController {
//
//    @Autowired
//    private UserService userService;
//
//    @PostMapping("/register")
//    public ResponseEntity<String> register(@RequestBody UserRegisterDTO dto) {
//        boolean ok = userService.register(dto);
//        return ok ? ResponseEntity.ok("注册成功")
//                : ResponseEntity.badRequest().body("用户名或邮箱已存在");
//    }
//}

//day03
package com.note.service.controller;

import com.note.service.common.util.JwtUtils;
import com.note.service.common.vo.Result;
import com.note.service.dto.LoginDTO;
import com.note.service.dto.UserRegisterDTO;
import com.note.service.entity.UserEntity;
import com.note.service.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/ums")
@Validated
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private JwtUtils jwtUtils;

    @PostMapping("/register")
    @Operation(summary="用户注册")
    public Result<String> register(@RequestBody @Valid UserRegisterDTO dto) {
        userService.register(dto);
        return Result.success("注册成功");

    }

    @PostMapping("/login")
    public Result<Map<String, Object>> login(@RequestBody @Valid LoginDTO dto) {
        UserEntity user = userService.login(dto);

        // 生成 JWT
        String token = jwtUtils.generateToken(user.getId(), user.getUsername());

        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        result.put("userId", user.getId());
        result.put("username", user.getUsername());

        return Result.success(result);
    }


    @GetMapping("/info")
    public Result<UserEntity> getUserInfo() {
        // 测试受保护接口，后续完善
        return Result.success(new UserEntity());
    }
}
````

## File: note-service/src/main/java/com/note/service/dto/UserRegisterDTO.java
````java
//day02
package com.note.service.dto;

import lombok.Data;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;

@Data
public class UserRegisterDTO {
    @NotBlank(message = "用户名不能为空")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 30, message = "密码长度 6-30 字符")
    private String password;

    @Email(message = "邮箱格式错误")
    @NotBlank(message = "邮箱不能为空")
    private String email;
}
````

## File: note-service/src/main/java/com/note/service/entity/UserEntity.java
````java
//day02
package com.note.service.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("user")
public class UserEntity {
    private Long id;
    private String username;
    private String password;
    private String email;
    private String avatarUrl;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
````

## File: note-service/src/main/java/com/note/service/mapper/UserMapper.java
````java
//day02
package com.note.service.mapper;


import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.note.service.entity.UserEntity;
import org.apache.ibatis.annotations.Mapper;
@Mapper
public interface UserMapper extends BaseMapper<UserEntity> {
}
````

## File: note-service/src/main/java/com/note/service/service/UserService.java
````java
//day02
//package com.note.service.service;
//
//import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
//import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
//import com.note.service.dto.UserRegisterDTO;
//import com.note.service.entity.UserEntity;
//import com.note.service.mapper.UserMapper;
//import org.springframework.stereotype.Service;
//@Service
//public class UserService extends ServiceImpl<UserMapper, UserEntity> {
//
//    public boolean register(UserRegisterDTO dto) {
//        // 1. 判重
//        boolean exist = baseMapper.selectCount(
//                new QueryWrapper<UserEntity>()
//                        .eq("username", dto.getUsername())
//                        .or()
//                        .eq("email", dto.getEmail())
//        ) > 0;
//        if (exist) return false;
//
//        // 2. 保存（密码先明文，Day3 再加密）
//        UserEntity user = new UserEntity();
//        user.setUsername(dto.getUsername());
//        user.setPassword(dto.getPassword());
//        user.setEmail(dto.getEmail());
//        return save(user);
//    }
//}

//day03-登录
package com.note.service.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.note.service.common.exception.BusinessException;
import com.note.service.dto.LoginDTO;
import com.note.service.dto.UserRegisterDTO;
import com.note.service.entity.UserEntity;
import com.note.service.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserService extends ServiceImpl<UserMapper, UserEntity> {

    @Autowired
    private PasswordEncoder passwordEncoder;

    public void register(UserRegisterDTO dto) {
        // 检查用户名
        if (lambdaQuery().eq(UserEntity::getUsername, dto.getUsername()).count() > 0) {
            throw new BusinessException(400, "用户名已存在");
        }

        // 检查邮箱
        if (lambdaQuery().eq(UserEntity::getEmail, dto.getEmail()).count() > 0) {
            throw new BusinessException(400, "邮箱已被注册");
        }

        // 创建用户（密码加密）
        UserEntity user = new UserEntity();
        user.setUsername(dto.getUsername());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setEmail(dto.getEmail());

        save(user);
    }

    public UserEntity login(LoginDTO dto) {
        UserEntity user = lambdaQuery()
                .eq(UserEntity::getUsername, dto.getUsername())
                .one();

        if (user == null || !passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
            throw new BusinessException(400, "用户名或密码错误");
        }

        return user;
    }
}
````

## File: note-ui/index.html
````html
<!doctype html>
<html lang="en">
  <head>
    <meta charset="UTF-8" />
    <link rel="icon" type="image/svg+xml" href="/vite.svg" />
    <meta name="viewport" content="width=device-width, initial-scale=1.0" />
    <title>note-ui</title>
  </head>
  <body>
    <div id="app"></div>
    <script type="module" src="/src/main.js"></script>
  </body>
</html>
````

## File: note-ui/package.json
````json
{
  "name": "note-ui",
  "private": true,
  "version": "0.0.0",
  "type": "module",
  "scripts": {
    "dev": "vite",
    "build": "tsc && vite build",
    "preview": "vite preview"
  },
  "devDependencies": {
    "@vitejs/plugin-vue": "^6.0.4",
    "typescript": "~5.9.3",
    "vite": "^7.2.4"
  },
  "dependencies": {
    "@element-plus/icons-vue": "^2.3.2",
    "axios": "^1.13.6",
    "element-plus": "^2.13.3",
    "highlight.js": "^11.11.1",
    "lodash-es": "^4.17.23",
    "marked": "^17.0.3",
    "pinia": "^3.0.4",
    "vue": "^3.5.29",
    "vue-router": "^4.6.4"
  }
}
````

## File: note-service/src/main/resources/application.yml
````yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/note_db?createDatabaseIfNotExist=true&serverTimezone=Asia/Shanghai
    username: root
    password: 1234
    driver-class-name: com.mysql.cj.jdbc.Driver

  data:
    redis:
      host: localhost
      port: 6379
      # password:      # 无密码留空
      database: 0
      lettuce:
        pool:
          max-active: 8
          max-idle: 8
          min-idle: 0
          max-wait: 100ms

mybatis-plus:
  configuration:
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl   # 控制台打印 SQL

jwt:
  secret: your-secret-key-note-lite-2024-very-long-key-at-least-32-bytes-ok
  expiration: 86400000  # 24小时，单位毫秒
````

## File: note-service/pom.xml
````xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
		 xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
		 xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
                             https://maven.apache.org/xsd/maven-4.0.0.xsd">
	<modelVersion>4.0.0</modelVersion>

	<parent>
		<groupId>org.springframework.boot</groupId>
		<artifactId>spring-boot-starter-parent</artifactId>
		<version>3.2.6</version>
		<relativePath/>
	</parent>

	<groupId>com.note</groupId>
	<artifactId>note-service</artifactId>
	<version>0.0.1-SNAPSHOT</version>
	<name>note-service</name>
	<description>note-service</description>

	<properties>
		<java.version>17</java.version>
		<mybatis-plus.version>3.5.7</mybatis-plus.version>
		<knife4j.version>4.4.0</knife4j.version>
	</properties>

	<dependencies>
		<!-- Web -->
		<dependency>
			<groupId>org.springframework.boot</groupId>
			<artifactId>spring-boot-starter-web</artifactId>
		</dependency>

		<!-- Validation -->
		<dependency>
			<groupId>org.springframework.boot</groupId>
			<artifactId>spring-boot-starter-validation</artifactId>
		</dependency>

		<!-- MyBatis-Plus（Spring Boot 3 专用版本） -->
		<dependency>
			<groupId>com.baomidou</groupId>
			<artifactId>mybatis-plus-spring-boot3-starter</artifactId>
			<version>${mybatis-plus.version}</version>
		</dependency>

		<!-- Knife4j（替代 Springfox，支持 OpenAPI 3 和 Jakarta） -->
		<dependency>
			<groupId>com.github.xiaoymin</groupId>
			<artifactId>knife4j-openapi3-jakarta-spring-boot-starter</artifactId>
			<version>${knife4j.version}</version>
		</dependency>

		<!-- MySQL -->
		<dependency>
			<groupId>com.mysql</groupId>
			<artifactId>mysql-connector-j</artifactId>
			<scope>runtime</scope>
		</dependency>

		<!-- Lombok -->
		<dependency>
			<groupId>org.projectlombok</groupId>
			<artifactId>lombok</artifactId>
			<optional>true</optional>
		</dependency>

		<!-- DevTools -->
		<dependency>
			<groupId>org.springframework.boot</groupId>
			<artifactId>spring-boot-devtools</artifactId>
			<scope>runtime</scope>
			<optional>true</optional>
		</dependency>

		<!-- 测试（修正artifactId） -->
		<dependency>
			<groupId>org.springframework.boot</groupId>
			<artifactId>spring-boot-starter-test</artifactId>
			<scope>test</scope>
		</dependency>

		<dependency>
			<groupId>io.jsonwebtoken</groupId>
			<artifactId>jjwt-api</artifactId>
			<version>0.12.3</version>
		</dependency>
		<dependency>
			<groupId>io.jsonwebtoken</groupId>
			<artifactId>jjwt-impl</artifactId>
			<version>0.12.3</version>
			<scope>runtime</scope>
		</dependency>
		<dependency>
			<groupId>io.jsonwebtoken</groupId>
			<artifactId>jjwt-jackson</artifactId>
			<version>0.12.3</version>
			<scope>runtime</scope>
		</dependency>

		<!-- Spring Security（若 Day1 未加） -->
		<dependency>
			<groupId>org.springframework.boot</groupId>
			<artifactId>spring-boot-starter-security</artifactId>
		</dependency>

		<!-- 参数校验（若 Day1 未加） -->


		<!--添加Redis依赖-->
		<dependency>
			<groupId>org.springframework.boot</groupId>
			<artifactId>spring-boot-starter-data-redis</artifactId>
		</dependency>
		<dependency>
			<groupId>org.apache.commons</groupId>
			<artifactId>commons-pool2</artifactId>
		</dependency>



<!--		&lt;!&ndash; SLF4J API &ndash;&gt;-->
<!--		<dependency>-->
<!--			<groupId>org.slf4j</groupId>-->
<!--			<artifactId>slf4j-api</artifactId>-->
<!--			<version>2.0.12</version>-->
<!--		</dependency>-->

<!--		&lt;!&ndash; 绑定具体日志实现（推荐 Logback） &ndash;&gt;-->
<!--		<dependency>-->
<!--			<groupId>ch.qos.logback</groupId>-->
<!--			<artifactId>logback-classic</artifactId>-->
<!--			<version>1.5.6</version>-->
<!--		</dependency>-->
	</dependencies>

	<build>
		<plugins>
			<plugin>
				<groupId>org.springframework.boot</groupId>
				<artifactId>spring-boot-maven-plugin</artifactId>
				<configuration>
					<excludes>
						<exclude>
							<groupId>org.projectlombok</groupId>
							<artifactId>lombok</artifactId>
						</exclude>
					</excludes>
				</configuration>
			</plugin>
		</plugins>
	</build>
</project>
````

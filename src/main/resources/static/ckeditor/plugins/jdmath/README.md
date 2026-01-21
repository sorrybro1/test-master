# CKeditor编辑器数学公式插件
基于CKeditor编辑器的可视化的数学公式编辑器，可以返回数学公式。

### 新增版本
由于公司的业务现在需要将线上的试卷弄到线下去使用，而现在的这个数学公式编辑器仅仅支持纯网页环境渲染公式。

导成word丢失css从而乱码

导成PDF，虽然样式保留了但是打印有机率文字断一半（应该是当图片处理的-我猜的）。

所以我们最理想的方案就选择了生成图片的公式编辑器。

以前用户百度的UE，记得上面的公式编辑器最后生成图片，于是想着移植到CK编辑器里面来吧。

所以新版本就这么诞生了。

这次的新版本支持公式的二次编辑（把ck官方插件中心的插件下载了个遍，研究了，然后把选中编辑弄出来了）。

#### 所以新项目就是->[gitee.com/jdmx/kityformula](https://gitee.com/jdmx/kityformula)

##### 这个项目还继续维护

##### 建议只有网页版的用户继续使用这个插件

# 演示地址
[http://jdmath.jdun.org/](http://jdmath.jdun.org/)

### 使用说明 

将
CKeditor编辑器的config.js打开，在
```
CKEDITOR.editorConfig = function( config ) {};
```
里面添加
```
config.extraPlugins = 'jdmath';
config.allowedContent = true;
```
然后在最后加入
```
CKEDITOR.config.contentsCss = '/ckeditor_4.7.3_full/ckeditor/plugins/jdmath/mathquill-0.10.1/mathquill.css';
```
以上是你编辑器的可视化部分，下面开始配置你的用户看到的页面的代码。
在客户看的页面添加css引入
```
<link rel="stylesheet" href="你的CKeditor路径/plugins/jdmath/mathquill-0.10.1/mathquill.css">
```
配置完后即可使用

本来我看着 [www.jmeditor.com](http://www.jmeditor.com/) 上面的JMEditor编辑器可以用，很兴奋，但是苦于作者长时间没更新，我的CKeditor编辑器的界面和功能需要其他的东西，所以决定自己写一个，于是乎参考了一下JMEditor，发现它是基于CKeditor的，所以我写的插件就这么成型了。
但是随后我发现如果这个公式编辑器过长则生成的HTML代码会把数据库撑满，于是乎我开始在寻找其他解决方案。

### 方案一

将公式只让公式编辑器返回LaTeX代码，然后重新进行渲染。看了CKeditor官方的公式插件就是这么做的，但是他的插入公式部分需要手写LaTeX代码。非常的不人性化。

### config.js代码参考
```
CKEDITOR.editorConfig = function( config ) {
    config.extraPlugins = 'jdmath';
    config.allowedContent = true;
};
CKEDITOR.config.contentsCss = '/ckeditor_4.7.3_full/ckeditor/plugins/jdmath/mathquill-0.10.1/mathquill.css';
```
![效果图](https://gitee.com/uploads/images/2017/1224/011506_ca49e80f_405677.png "QQ图片20171224011341.png")
/**
 * @license Copyright (c) 2003-2013, CKSource - Frederico Knabben. All rights reserved.
 * For licensing, see LICENSE.html or http://ckeditor.com/license
 */

CKEDITOR.editorConfig = function( config ) {
	// Define changes to default configuration here. For example:
	// config.language = 'fr';
	// config.uiColor = '#AADC6E';
	// 工具栏（基础'Basic'、全能'Full'、自定义）plugins/toolbar/plugin.js

	config.extraPlugins = 'jdmath';
	config.allowedContent = true;

   config.toolbar = 'Basic';

    config.toolbar = 'Full';
	config.toolbar_Full = [
		['Preview'],
		['Cut','Copy','Paste','PasteText','PasteFromWord','-', 'SpellChecker', 'Scayt'],
		['Undo','Redo','-','Find','Replace','-','SelectAll','RemoveFormat'],
		['Form', 'Checkbox', 'Radio', 'TextField', 'Textarea', 'Select', 'Button', 'ImageButton', 'HiddenField'],
		['Bold','Italic','Underline','Strike','-','Subscript','Superscript'],
		['NumberedList','BulletedList','-','Outdent','Indent'],
		['JustifyLeft','JustifyCenter','JustifyRight','JustifyBlock'],
		['Link','Unlink'],
		['Image','Table','SpecialChar',],
		['Styles','Format','Font','FontSize'],
		['TextColor','BGColor','jdmath']

	];

		config.extraPlugins += (config.extraPlugins ? ',jdmath' : 'jdmath');

		config.removeButtons = 'Underline,Subscript,Superscript';

		// Set the most common block elements.
		config.format_tags = 'p;h1;h2;h3;pre';

		// Simplify the dialog windows.
		config.removeDialogTabs = 'image:advanced;link:advanced';

		config.filebrowserImageUploadUrl = "../image/imageUpload.do";
		config.filebrowserUploadUrl ="../image/filesUpload.do";

		/*CKEDITOR.*/config.contentsCss = '../plugins/jdmath/mathquill-0.10.1/mathquill.css';

    CKEDITOR.editorConfig = function( config ) {
        config.allowedContent = true;

        // 关键：指向后端上传接口
        config.filebrowserImageUploadUrl = '/admin-api/uploads/image';
        config.filebrowserUploadUrl = '/admin-api/uploads/file';
    };
};

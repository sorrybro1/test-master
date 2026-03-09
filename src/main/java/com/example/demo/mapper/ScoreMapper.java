package com.example.demo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.demo.dto.CourseStudentRowVO;
import com.example.demo.entity.Score;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Mapper
public interface ScoreMapper extends BaseMapper<Score> {

    // ==========================================
    // 1. 下拉框与基础查询
    // ==========================================

    /**
     * [对应 /getSemester.do]
     * 获取 st_course 表中所有不重复的 semester
     */
    List<LocalDateTime> selectAllSemesters();

    /**
     * [对应 /getStdentSemester.do]
     * 获取指定学生有成绩/课程的学期
     */
    List<LocalDateTime> selectStudentSemesters(@Param("uid") String uid);

    List<LocalDateTime> getStdentSemester(@Param("uid") String uid);
    /**
     * [对应 /getTotalScoreTime.do]
     * 获取总成绩统计里涉及的所有学期/批次
     */
    List<LocalDateTime> selectTotalScoreTimes();

    // ==========================================
    // 2. 课程管理 (Curriculum)
    // ==========================================

    /**
     * [对应 /curriculumList.do] 统计总数
     */
    // 对应 curriculumList.do
    long countCurriculum(@Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);

    List<Map<String, Object>> listCurriculum(@Param("startTime") LocalDateTime startTime,
                                             @Param("endTime") LocalDateTime endTime,
                                             @Param("offset") int offset,
                                             @Param("limit") int limit);

    /**
     * [对应 /seeCurriculum.do]
     */
    Map<String, Object> selectCurriculumById(@Param("id") Long id);

    /**
     * [对应 /updateEndingtime.do]
     * 原逻辑是用 cnumber 更新
     */
    int updateCourseEndingTime(@Param("cNumber") String cNumber,
                               @Param("semester") String semester,
                               @Param("time") String time);

    /**
     * [对应 /deleteCurriculum.do]
     * 注意：不仅要删 st_course，还要删 st_orgcourse(绑定关系) 和 st_score(成绩)
     * 这里先定义删主表，XML 中可以配置多语句或者在 Service 层处理，这里假设只删课程
     */
    void deleteCurriculumById(@Param("id") String id);

    // 获取指定课程ID下所有非空的上传文件名
    List<String> selectFileNamesByCourseId(String courseId);

    // ==========================================
    // 3. 学生端课程 (CourseStudent)
    // ==========================================

    /**
     * [对应 /courseStudentList.do]
     */
    long countCourseStudent(@Param("uid") String uid,
                            @Param("startTime") LocalDateTime startTime,
                            @Param("endTime") LocalDateTime endTime);

    List<CourseStudentRowVO> listCourseStudent(@Param("uid") String uid,
                                               @Param("startTime") LocalDateTime startTime,
                                               @Param("endTime") LocalDateTime endTime,
                                               @Param("offset") int offset,
                                               @Param("limit") int limit);

    // ==========================================
    // 4. 文件上传 (Upload)
    // ==========================================

    /**
     * [对应 /uploadReport.do]
     * 查询旧文件名用于删除
     */
    String selectUploadFileName(@Param("scoId") Long scoId);

    /**
     * [对应 /uploadReport.do]
     * 更新文件名和提交时间
     */
    void updateUploadInfo(@Param("scoId") Long scoId,
                          @Param("fileName") String fileName,
                          @Param("submitTime") LocalDateTime submitTime);

    // ==========================================
    // 5. 实验报告与评分 (LaboratoryReport)
    // ==========================================

    /**
     * [对应 /laboratoryReportList.do]
     */
    long countLaboratoryReports(@Param("cname") String cname, @Param("cnumber") String cnumber);

    /**
     * [对应 /laboratoryReportList.do]
     * 关联 st_user, st_course, st_orgrole 查询详细信息
     */
    List<Map<String, Object>> listLaboratoryReports(@Param("cname") String cname,
                                                    @Param("cnumber") String cnumber,
                                                    @Param("offset") int offset,
                                                    @Param("limit") int limit);

    /**
     * [对应 /saveScoring.do]
     */
    void updateScore(@Param("scoId") Long scoId,
                     @Param("degreeExp") Double degreeExp,
                     @Param("degreeOther") Double degreeOther,
                     @Param("totalPoints") Double totalPoints);

    /**
     * [对应 /downloadCheckLaboratoryReport.do]
     * 批量下载：根据ID列表查文件
     */
    List<Map<String, String>> selectFilesByScoIds(@Param("ids") List<Long> ids);

    /**
     * [对应 /downloadLaboratoryReport.do]
     * 单个下载
     */
    Map<String, String> selectFileByScoId(@Param("scoId") Long scoId);

    // ==========================================
    // 6. 总成绩 (TotalScore)
    // ==========================================

    /**
     * [对应 /getTotalScoreList.do]
     */
    long countTotalScores(@Param("time") String time);

    /**
     * [对应 /getTotalScoreList.do]
     * 查询总成绩视图
     */
    List<Map<String, Object>> listTotalScores(@Param("time") String time,
                                              @Param("offset") int offset,
                                              @Param("limit") int limit);

    /**
     * [对应 /excelTotalScore.do]
     * 导出 Excel 用的全量数据
     */
    List<Map<String, Object>> selectTotalScoreForExport();
}
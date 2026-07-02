package com.rajeducational.erp.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.rajeducational.erp.ui.landing.*
import com.rajeducational.erp.ui.student.*
import com.rajeducational.erp.ui.teacher.*
import com.rajeducational.erp.ui.staff.*
import com.rajeducational.erp.ui.guest.*
import com.rajeducational.erp.ui.admin.*

@Composable
fun AppNavigation(startDestination: String = "landing") {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = startDestination) {
        composable("landing") { LandingScreen(navController) }
        composable("non_attending_student_auth") { NonAttendingStudentAuthScreen(navController) }
        composable("student_auth") { StudentAuthScreen(navController) }
        composable("teacher_auth") { TeacherAuthScreen(navController) }
        composable("student_dashboard") { StudentDashboardScreen(navController) }
        composable("student_profile") { StudentProfileScreen(navController) }
        composable("student_fees") { StudentFeesScreen(navController) }
        composable("student_ratings") { StudentRatingsScreen(navController) }
        composable("student_gallery") { StudentGalleryScreen(navController) }
        composable("student_voting") { StudentVotingScreen(navController) }
        composable("student_messages") { StudentMessagesScreen(navController) }
        composable("student_notices") { StudentNoticesScreen(navController) }
        composable("student_events") { StudentEventsScreen(navController) }
        composable("student_event_detail/{eventId}") { backStackEntry -> 
            val eventId = backStackEntry.arguments?.getString("eventId") ?: ""
            StudentEventDetailScreen(navController, eventId)
        }
        composable("face_registration/{userType}/{userId}") { backStackEntry ->
            val userType = backStackEntry.arguments?.getString("userType") ?: "student"
            val userId = backStackEntry.arguments?.getString("userId") ?: ""
            com.rajeducational.erp.ui.face.FaceRegistrationScreen(navController, userType, userId)
        }
        composable("teacher_registration") { com.rajeducational.erp.ui.teacher.TeacherRegistrationScreen(navController) }
        composable("teacher_login") { com.rajeducational.erp.ui.teacher.TeacherLoginScreen(navController) }
        composable("teacher_announcements") { com.rajeducational.erp.ui.teacher.TeacherAnnouncementsScreen(navController) }
        composable("teacher_dashboard") { com.rajeducational.erp.ui.teacher.TeacherDashboardScreen(navController) }
        composable("teacher_daily_teaching_plan") { com.rajeducational.erp.ui.teacher.TeacherDailyTeachingPlanScreen(navController) }
        composable("teacher_gallery") { com.rajeducational.erp.ui.teacher.TeacherGalleryScreen(navController) }
        composable("teacher_students") { TeacherStudentsScreen(navController) }
        composable("teacher_all_students") { TeacherAllStudentsScreen(navController) }
        composable("teacher_reports") { TeacherReportsScreen(navController) }
        composable("teacher_ratings") { TeacherRatingsScreen(navController) }
        composable("teacher_student_ratings_detail") { com.rajeducational.erp.ui.teacher.TeacherStudentRatingsDetailScreen(navController) }
        composable("teacher_management_ratings_detail") { com.rajeducational.erp.ui.teacher.TeacherManagementRatingsDetailScreen(navController) }
        composable("teacher_profile") { com.rajeducational.erp.ui.teacher.TeacherProfileScreen(navController) }
        composable("teacher_more") { com.rajeducational.erp.ui.teacher.TeacherMoreScreen(navController) }
        composable("teacher_voting") { com.rajeducational.erp.ui.teacher.TeacherVotingScreen(navController) }
        composable("teacher_attendance_control_center") { com.rajeducational.erp.ui.teacher.TeacherAttendanceControlCenter(navController) }
        composable("teacher_attendance_settings") { com.rajeducational.erp.ui.teacher.TeacherAttendanceSettingsScreen(navController) }
        composable("teacher_attendance_report_control") { com.rajeducational.erp.ui.teacher.TeacherAttendanceReportControlScreen(navController) }
        composable("teacher_attendance_report_individual") { com.rajeducational.erp.ui.teacher.TeacherAttendanceReportIndividualScreen(navController) }
        composable("teacher_attendance_report_class") { com.rajeducational.erp.ui.teacher.TeacherAttendanceReportClassScreen(navController) }
        composable("face_attendance_scanner/{type}") { backStackEntry ->
            val type = backStackEntry.arguments?.getString("type") ?: "IN"
            com.rajeducational.erp.ui.face.FaceAttendanceScreen(navController, type)
        }
        composable("teacher_send_announcement") { com.rajeducational.erp.ui.teacher.TeacherSendAnnouncementScreen(navController) }
        composable("teacher_send_local_announcement") { com.rajeducational.erp.ui.teacher.TeacherSendLocalAnnouncementScreen(navController) }
        composable("teacher_student_qr_registration") { com.rajeducational.erp.ui.teacher.TeacherStudentQRRegistrationScreen(navController) }
        composable("teacher_non_attending_approvals") { com.rajeducational.erp.ui.teacher.TeacherNonAttendingApprovalsScreen(navController) }
        composable("teacher_attending_approvals") { com.rajeducational.erp.ui.teacher.TeacherAttendingApprovalsScreen(navController) }
        composable("teacher_messages") { com.rajeducational.erp.ui.teacher.TeacherMessagesScreen(navController) }
        composable("staff_auth") { StaffAuthScreen(navController) }

        composable("staff_registration") { com.rajeducational.erp.ui.staff.StaffRegistrationScreen(navController) }
        composable("staff_login") { com.rajeducational.erp.ui.staff.StaffLoginScreen(navController) }
        composable("staff_announcements") { com.rajeducational.erp.ui.staff.StaffAnnouncementsScreen(navController) }
        composable("staff_dashboard") { com.rajeducational.erp.ui.staff.StaffDashboardScreen(navController) }
        composable("staff_gallery") { com.rajeducational.erp.ui.staff.StaffGalleryScreen(navController) }
        composable("staff_students") { StaffStudentsScreen(navController) }
        composable("staff_all_students") { StaffAllStudentsScreen(navController) }
        composable("staff_reports") { StaffReportsScreen(navController) }
        composable("staff_ratings") { StaffRatingsScreen(navController) }
        composable("staff_student_ratings_detail") { com.rajeducational.erp.ui.staff.StaffStudentRatingsDetailScreen(navController) }
        composable("staff_management_ratings_detail") { com.rajeducational.erp.ui.staff.StaffManagementRatingsDetailScreen(navController) }
        composable("staff_profile") { com.rajeducational.erp.ui.staff.StaffProfileScreen(navController) }
        composable("staff_more") { com.rajeducational.erp.ui.staff.StaffMoreScreen(navController) }
        composable("staff_voting") { com.rajeducational.erp.ui.staff.StaffVotingScreen(navController) }
        composable("staff_attendance_control_center") { com.rajeducational.erp.ui.staff.StaffAttendanceControlCenter(navController) }
        composable("staff_attendance_settings") { com.rajeducational.erp.ui.staff.StaffAttendanceSettingsScreen(navController) }
        composable("staff_attendance_report_control") { com.rajeducational.erp.ui.staff.StaffAttendanceReportControlScreen(navController) }
        composable("staff_attendance_report_individual") { com.rajeducational.erp.ui.staff.StaffAttendanceReportIndividualScreen(navController) }
        composable("staff_attendance_report_class") { com.rajeducational.erp.ui.staff.StaffAttendanceReportClassScreen(navController) }

        composable("staff_send_announcement") { com.rajeducational.erp.ui.staff.StaffSendAnnouncementScreen(navController) }
        composable("staff_send_local_announcement") { com.rajeducational.erp.ui.staff.StaffSendLocalAnnouncementScreen(navController) }
        composable("staff_student_qr_registration") { com.rajeducational.erp.ui.staff.StaffStudentQRRegistrationScreen(navController) }
        composable("staff_messages") { com.rajeducational.erp.ui.staff.StaffMessagesScreen(navController) }
        composable("guest_portal") { GuestPortalScreen(navController) }
        composable("guest_courses") { GuestCoursesScreen(navController) }
        composable("guest_fees") { GuestFeesScreen(navController) }
        composable("guest_college_fee_detail/{collegeId}") { backStackEntry ->
            val collegeId = backStackEntry.arguments?.getString("collegeId") ?: ""
            GuestCollegeFeeDetailScreen(navController, collegeId)
        }
        composable("guest_gallery") { GuestGalleryScreen(navController) }
        composable("guest_events") { GuestEventsScreen(navController) }
        composable("guest_contact") { GuestContactScreen(navController) }
        composable("guest_college_detail/{collegeId}") { backStackEntry ->
            val collegeId = backStackEntry.arguments?.getString("collegeId") ?: ""
            GuestCollegeDetailScreen(navController, collegeId)
        }
        composable("admin_panel") { AdminPanelScreen(navController) }
        composable("admin_college_control") { CollegeControlCentreScreen(navController) }
        composable("admin_fee_structure_control") { com.rajeducational.erp.ui.admin.FeeStructureControlScreen(navController) }
        composable("admin_fee_course_list/{collegeId}") { backStackEntry ->
            val collegeId = backStackEntry.arguments?.getString("collegeId") ?: ""
            com.rajeducational.erp.ui.admin.FeeCourseListScreen(navController, collegeId)
        }
        composable("admin_gallery_upload_center") { com.rajeducational.erp.ui.admin.AdminGalleryUploadScreen(navController) }
        composable("admin_event_control_center") { com.rajeducational.erp.ui.admin.AdminEventControlScreen(navController) }
        composable("admin_statistics_control") { com.rajeducational.erp.ui.admin.AdminStatisticsControlScreen(navController) }
        composable("admin_developer_options") { com.rajeducational.erp.ui.admin.DeveloperOptionsScreen(navController) }
        composable("admin_give_holiday") { com.rajeducational.erp.ui.admin.AdminGiveHolidayScreen(navController) }
        composable("admin_announcement_control") { com.rajeducational.erp.ui.admin.AdminAnnouncementControlScreen(navController) }
        composable("admin_view_students") { com.rajeducational.erp.ui.admin.AdminViewStudentsScreen(navController) }
        composable("admin_council_voting_control") { com.rajeducational.erp.ui.admin.AdminCouncilVotingControlScreen(navController) }
        composable("admin_council_voting_stats") { com.rajeducational.erp.ui.admin.AdminCouncilVotingStatsScreen(navController) }
        composable("admin_teacher_review_criteria_control") { com.rajeducational.erp.ui.admin.AdminTeacherReviewCriteriaControlScreen(navController) }
        composable("admin_teacher_reviews") { com.rajeducational.erp.ui.admin.AdminTeacherReviewsScreen(navController) }
        composable("admin_management_review_control") { com.rajeducational.erp.ui.admin.AdminManagementReviewControlScreen(navController) }
        composable("admin_management_review_criteria_control") { com.rajeducational.erp.ui.admin.AdminManagementReviewCriteriaControlScreen(navController) }
        composable("admin_teacher_qr_control") { com.rajeducational.erp.ui.admin.AdminTeacherQRControlScreen(navController) }
        composable("admin_registered_teachers") { com.rajeducational.erp.ui.admin.AdminRegisteredTeachersScreen(navController) }
        composable("admin_teacher_students/{teacherId}") { backStackEntry ->
            val teacherId = backStackEntry.arguments?.getString("teacherId") ?: ""
            com.rajeducational.erp.ui.admin.AdminTeacherStudentsScreen(navController, teacherId)
        }
        composable("admin_guest_messages_control") { com.rajeducational.erp.ui.admin.AdminGuestMessagesScreen(navController) }
        composable("admin_student_messages_control") { com.rajeducational.erp.ui.admin.AdminStudentMessagesScreen(navController) }
        composable("admin_fee_reminder_control") { com.rajeducational.erp.ui.admin.AdminFeeReminderControlScreen(navController) }
        composable("admin_teacher_messages") { com.rajeducational.erp.ui.admin.AdminTeacherMessagesScreen(navController) }
        composable("admin_teacher_staff_attendance_control") { com.rajeducational.erp.ui.admin.AdminTeacherStaffAttendanceControlScreen(navController) }
        composable("admin_daily_teaching_plan_control") { com.rajeducational.erp.ui.admin.AdminDailyTeachingPlanControlScreen(navController) }
        composable("face_attendance_scanner_ts/{type}") { backStackEntry ->
            val type = backStackEntry.arguments?.getString("type") ?: "IN"
            com.rajeducational.erp.ui.face.FaceAttendanceTeacherStaffScreen(navController, type)
        }
        composable("admin_add_admin") { com.rajeducational.erp.ui.admin.AdminAddAdminScreen(navController) }
        composable("admin_generate_reports") { com.rajeducational.erp.ui.admin.AdminGenerateReportScreen(navController) }
        composable("admin_monitor_admin") { com.rajeducational.erp.ui.admin.AdminMonitorAdminsScreen(navController) }
        composable("student_teacher_reviews") { com.rajeducational.erp.ui.student.StudentTeacherReviewScreen(navController) }
        composable("guest_event_detail/{eventId}") { backStackEntry -> 
            val eventId = backStackEntry.arguments?.getString("eventId") ?: ""
            com.rajeducational.erp.ui.guest.GuestEventDetailScreen(navController, eventId)
        }
    }
}

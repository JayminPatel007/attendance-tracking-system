//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//
// @dart=2.18

// ignore_for_file: unused_element, unused_import
// ignore_for_file: always_put_required_named_parameters_first
// ignore_for_file: constant_identifier_names
// ignore_for_file: lines_longer_than_80_chars

library openapi.api;

import 'dart:async';
import 'dart:convert';
import 'dart:io';

import 'package:collection/collection.dart';
import 'package:http/http.dart';
import 'package:intl/intl.dart';
import 'package:meta/meta.dart';

part 'api_client.dart';
part 'api_helper.dart';
part 'api_exception.dart';
part 'auth/authentication.dart';
part 'auth/api_key_auth.dart';
part 'auth/oauth.dart';
part 'auth/http_basic_auth.dart';
part 'auth/http_bearer_auth.dart';

part 'api/attendance_rest_controller_api.dart';
part 'api/audit_log_bff_controller_api.dart';
part 'api/bff_session_controller_api.dart';
part 'api/dashboard_bff_controller_api.dart';
part 'api/directory_bff_controller_api.dart';
part 'api/home_sabha_transfer_rest_controller_api.dart';
part 'api/identity_rest_controller_api.dart';
part 'api/occurrence_reopen_bff_controller_api.dart';
part 'api/password_reissue_controller_api.dart';
part 'api/password_reset_rest_controller_api.dart';
part 'api/person_directory_rest_controller_api.dart';
part 'api/role_appointment_controller_api.dart';
part 'api/sabha_definition_controller_api.dart';
part 'api/sabha_list_controller_api.dart';
part 'api/sanchalak_proxy_bff_controller_api.dart';
part 'api/selection_bff_controller_api.dart';
part 'api/selection_rest_controller_api.dart';
part 'api/structural_creation_controller_api.dart';
part 'api/structural_deletion_controller_api.dart';
part 'api/who_appointed_me_rest_controller_api.dart';

part 'model/add_person_request.dart';
part 'model/add_person_response.dart';
part 'model/appointee_payload.dart';
part 'model/appointer_contact.dart';
part 'model/appointment_request.dart';
part 'model/appointment_response.dart';
part 'model/audit_entry.dart';
part 'model/cancel_request.dart';
part 'model/candidate_row.dart';
part 'model/choose_city_request.dart';
part 'model/city_chip.dart';
part 'model/city_option.dart';
part 'model/city_view.dart';
part 'model/complete_payload.dart';
part 'model/confirm_transfer_request.dart';
part 'model/create_city_request.dart';
part 'model/create_kshetra_request.dart';
part 'model/create_occurrence_request.dart';
part 'model/create_sabha_kind_request.dart';
part 'model/create_zone_request.dart';
part 'model/created_occurrence_response.dart';
part 'model/created_response.dart';
part 'model/current_occurrence.dart';
part 'model/current_roster.dart';
part 'model/dashboard_overview.dart';
part 'model/define_sabha_request.dart';
part 'model/deselect_request.dart';
part 'model/initiate_transfer_request.dart';
part 'model/initiate_transfer_response.dart';
part 'model/kpis.dart';
part 'model/kshetra.dart';
part 'model/kshetra_view.dart';
part 'model/mark_request.dart';
part 'model/marking_item.dart';
part 'model/monthly_compliance_response.dart';
part 'model/monthly_sabha.dart';
part 'model/name_candidate.dart';
part 'model/new_person_payload.dart';
part 'model/nominate_request.dart';
part 'model/nominate_response.dart';
part 'model/occurrence_view.dart';
part 'model/pending_nomination_item.dart';
part 'model/person_response.dart';
part 'model/proxy_occurrence_item.dart';
part 'model/proxy_sabha_list_item.dart';
part 'model/reissue_request.dart';
part 'model/reject_request.dart';
part 'model/reopen_list_item.dart';
part 'model/reopen_request.dart';
part 'model/request_payload.dart';
part 'model/request_response.dart';
part 'model/reschedule_request.dart';
part 'model/roster_entry.dart';
part 'model/sabha.dart';
part 'model/sabha_definition_response.dart';
part 'model/sabha_kind_view.dart';
part 'model/sabha_tree.dart';
part 'model/sabha_view.dart';
part 'model/sah_nirdeshak_cap_response.dart';
part 'model/selected_person_item.dart';
part 'model/sync_request.dart';
part 'model/sync_response.dart';
part 'model/thresholds.dart';
part 'model/thresholds_request.dart';
part 'model/venue_override_request.dart';
part 'model/verify_payload.dart';
part 'model/verify_response.dart';
part 'model/walk_in_candidate.dart';
part 'model/walk_in_request.dart';
part 'model/web_session_response.dart';
part 'model/who_am_i_response.dart';
part 'model/who_appointed_me_response.dart';
part 'model/zone.dart';
part 'model/zone_view.dart';


/// An [ApiClient] instance that uses the default values obtained from
/// the OpenAPI specification file.
var defaultApiClient = ApiClient();

const _delimiters = {'csv': ',', 'ssv': ' ', 'tsv': '\t', 'pipes': '|'};
const _dateEpochMarker = 'epoch';
const _deepEquality = DeepCollectionEquality();
final _dateFormatter = DateFormat('yyyy-MM-dd');
final _regList = RegExp(r'^List<(.*)>$');
final _regSet = RegExp(r'^Set<(.*)>$');
final _regMap = RegExp(r'^Map<String,(.*)>$');

bool _isEpochMarker(String? pattern) => pattern == _dateEpochMarker || pattern == '/$_dateEpochMarker/';
